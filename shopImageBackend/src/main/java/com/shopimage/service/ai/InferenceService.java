package com.shopimage.service.ai;

import ai.onnxruntime.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InferenceService {

    public static class Prediction {
        public Long categoryId;
        public Double confidence;

        public Prediction(Long categoryId, Double confidence) {
            this.categoryId = categoryId;
            this.confidence = confidence;
        }
    }

    // 映射 ONNX 输出索引 -> 业务类目ID，可通过配置覆盖
    // 默认: 0->1(裤子), 1->4(鞋子), 2->3(上衣)
    @Value("${ai.outputIndexToCategory:0=1,1=4,2=3}")
    private String outputIndexToCategoryConfig;

    private Map<Integer, Long> getOutputMapping() {
        Map<Integer, Long> map = new HashMap<>();
        try {
            String[] pairs = outputIndexToCategoryConfig.split(",");
            for (String p : pairs) {
                String[] kv = p.split("=");
                map.put(Integer.parseInt(kv[0].trim()), Long.parseLong(kv[1].trim()));
            }
        } catch (Exception e) {
            log.warn("Failed to parse ai.outputIndexToCategory config: {}", outputIndexToCategoryConfig);
            map.put(0, 1L);
            map.put(1, 4L);
            map.put(2, 3L);
        }
        return map;
    }

    public List<Prediction> infer(String modelPath, float[] inputNhwc, int topN) {
        try {
            if (!Files.exists(Path.of(modelPath))) {
                throw new RuntimeException("Model file not found: " + modelPath);
            }

            // ResNet 常用 NCHW 输入，做 NHWC->NCHW，且做 ImageNet 归一化
            float[] nchw = nhwcToNchwAndNormalize(inputNhwc, 224, 224);

            // 直接使用 ONNX Runtime API
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            
            try (OrtSession session = env.createSession(modelPath, opts)) {
                // 构造输入张量
                long[] shape = new long[] { 1, 3, 224, 224 };
                OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(nchw), shape);
                
                // 获取输入名
                String inputName = session.getInputNames().iterator().next();
                
                // 执行推理
                Map<String, OnnxTensor> inputs = Map.of(inputName, inputTensor);
                try (OrtSession.Result result = session.run(inputs)) {
                    // 获取输出
                    OnnxValue output = result.get(0);
                    float[][] outputArray = (float[][]) output.getValue();
                    float[] logits = outputArray[0]; // 取第一个batch的结果
                    
                    double[] probs = softmax(logits);
                    List<Integer> indices = topK(probs, topN);
                    Map<Integer, Long> mapping = getOutputMapping();
                    List<Prediction> predictions = new ArrayList<>();
                    for (int idx : indices) {
                        Long categoryId = mapping.getOrDefault(idx, null);
                        predictions.add(new Prediction(categoryId, probs[idx]));
                    }
                    return predictions;
                }
            }
        } catch (OrtException e) {
            log.error("ONNX Runtime error: {}", e.getMessage(), e);
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("ONNX inference failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Prediction getTopPrediction(String modelPath, float[] inputData) {
        List<Prediction> predictions = infer(modelPath, inputData, 1);
        return predictions.isEmpty() ? null : predictions.get(0);
    }

    private static float[] nhwcToNchwAndNormalize(float[] nhwc, int h, int w) {
        // nhwc length = h*w*3
        float[] nchw = new float[3 * h * w];
        // ImageNet 均值/方差
        float[] mean = new float[] { 0.485f, 0.456f, 0.406f };
        float[] std = new float[] { 0.229f, 0.224f, 0.225f };
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float r = (nhwc[idx++] + 1f) / 2f; // 归一到[0,1]
                float g = (nhwc[idx++] + 1f) / 2f;
                float b = (nhwc[idx++] + 1f) / 2f;
                int offset = y * w + x;
                nchw[0 * h * w + offset] = (r - mean[0]) / std[0];
                nchw[1 * h * w + offset] = (g - mean[1]) / std[1];
                nchw[2 * h * w + offset] = (b - mean[2]) / std[2];
            }
        }
        return nchw;
    }

    private static double[] softmax(float[] logits) {
        double max = Double.NEGATIVE_INFINITY;
        for (float v : logits)
            max = Math.max(max, v);
        double sum = 0.0;
        double[] exps = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            exps[i] = Math.exp(logits[i] - max);
            sum += exps[i];
        }
        for (int i = 0; i < exps.length; i++)
            exps[i] /= sum;
        return exps;
    }

    private static List<Integer> topK(double[] probs, int k) {
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[1], b[1]));
        for (int i = 0; i < probs.length; i++) {
            double score = probs[i];
            if (pq.size() < k) {
                pq.offer(new double[] { i, score });
            } else if (score > pq.peek()[1]) {
                pq.poll();
                pq.offer(new double[] { i, score });
            }
        }
        List<Integer> result = new ArrayList<>();
        while (!pq.isEmpty())
            result.add((int) pq.poll()[0]);
        Collections.reverse(result);
        return result;
    }
}
