"""
MinIO对象存储客户端配置和操作模块
"""
from minio import Minio
from minio.error import S3Error
import os
from datetime import datetime, timedelta
from pathlib import Path
import io
from urllib.parse import urljoin

class MinIOClient:
    def __init__(self):
        # MinIO配置 - 与Java后端保持一致
        self.endpoint = "114.66.57.6:9000"
        self.access_key = "admin"
        self.secret_key = "admin123456"
        self.bucket_name = "shop-images"
        self.secure = False  # HTTP连接
        
        # 初始化MinIO客户端
        self.client = Minio(
            self.endpoint,
            access_key=self.access_key,
            secret_key=self.secret_key,
            secure=self.secure
        )
        
        # 确保bucket存在
        self._ensure_bucket_exists()
    
    def _ensure_bucket_exists(self):
        """确保bucket存在，如果不存在则创建"""
        try:
            if not self.client.bucket_exists(self.bucket_name):
                self.client.make_bucket(self.bucket_name)
                print(f"创建bucket: {self.bucket_name}")
            else:
                print(f"Bucket {self.bucket_name} 已存在")
        except S3Error as e:
            print(f"检查/创建bucket失败: {e}")
    
    def upload_file(self, file_path, object_name, content_type=None):
        """
        上传文件到MinIO
        
        Args:
            file_path (str): 本地文件路径
            object_name (str): MinIO中的对象名称
            content_type (str): 文件MIME类型
        
        Returns:
            str: 上传成功返回对象URL，失败返回None
        """
        try:
            # 自动检测content_type
            if content_type is None:
                if object_name.endswith('.pt'):
                    content_type = 'application/octet-stream'
                elif object_name.endswith(('.jpg', '.jpeg')):
                    content_type = 'image/jpeg'
                elif object_name.endswith('.png'):
                    content_type = 'image/png'
                elif object_name.endswith('.zip'):
                    content_type = 'application/zip'
                else:
                    content_type = 'application/octet-stream'
            
            # 上传文件
            self.client.fput_object(
                self.bucket_name,
                object_name,
                file_path,
                content_type=content_type
            )
            
            # 返回文件URL
            file_url = f"http://{self.endpoint}/{self.bucket_name}/{object_name}"
            print(f"文件上传成功: {file_url}")
            return file_url
            
        except S3Error as e:
            print(f"文件上传失败: {e}")
            return None
        except Exception as e:
            print(f"上传过程中发生错误: {e}")
            return None
    
    def upload_bytes(self, data, object_name, content_type=None):
        """
        上传字节数据到MinIO
        
        Args:
            data (bytes): 要上传的字节数据
            object_name (str): MinIO中的对象名称
            content_type (str): 文件MIME类型
        
        Returns:
            str: 上传成功返回对象URL，失败返回None
        """
        try:
            print(f"开始上传到MinIO: {object_name}")
            print(f"数据大小: {len(data)} 字节")
            print(f"MinIO服务器: {self.endpoint}")
            print(f"Bucket: {self.bucket_name}")
            
            # 自动检测content_type
            if content_type is None:
                if object_name.endswith(('.jpg', '.jpeg')):
                    content_type = 'image/jpeg'
                elif object_name.endswith('.png'):
                    content_type = 'image/png'
                else:
                    content_type = 'application/octet-stream'
            
            print(f"Content-Type: {content_type}")
            
            # 将字节数据转换为文件对象
            data_stream = io.BytesIO(data)
            print("数据流创建完成，开始上传...")
            
            # 上传数据
            self.client.put_object(
                self.bucket_name,
                object_name,
                data_stream,
                length=len(data),
                content_type=content_type
            )
            
            print("MinIO put_object 调用完成")
            
            # 返回文件URL
            file_url = f"http://{self.endpoint}/{self.bucket_name}/{object_name}"
            print(f"数据上传成功: {file_url}")
            return file_url
            
        except S3Error as e:
            print(f"MinIO S3错误: {e}")
            print(f"错误代码: {e.code if hasattr(e, 'code') else 'N/A'}")
            print(f"错误消息: {e.message if hasattr(e, 'message') else str(e)}")
            return None
        except Exception as e:
            print(f"上传过程中发生未知错误: {type(e).__name__}: {e}")
            import traceback
            print(f"错误堆栈: {traceback.format_exc()}")
            return None
    
    def download_file(self, object_name, file_path, max_retries=3):
        """
        从MinIO下载文件（带重试机制）
        
        Args:
            object_name (str): MinIO中的对象名称
            file_path (str): 本地保存路径
            max_retries (int): 最大重试次数
        
        Returns:
            bool: 下载成功返回True，失败返回False
        """
        import time
        
        # 检查文件路径是否有效
        if not file_path or file_path.strip() == '':
            print(f"错误: 文件路径为空或无效: '{file_path}'")
            return False
        
        for attempt in range(max_retries):
            try:
                print(f"开始下载文件 (尝试 {attempt + 1}/{max_retries}): {object_name}")
                print(f"目标路径: {file_path}")
                
                # 确保目标目录存在 - 只有当路径包含目录时才创建
                dir_path = os.path.dirname(file_path)
                if dir_path and dir_path.strip():
                    os.makedirs(dir_path, exist_ok=True)
                
                # 使用get_object获取对象流，然后手动写入文件
                response = self.client.get_object(self.bucket_name, object_name)
                
                # 获取文件大小
                stat = self.client.stat_object(self.bucket_name, object_name)
                total_size = stat.size
                print(f"文件大小: {total_size} 字节")
                
                # 分块下载并写入文件
                with open(file_path, 'wb') as f:
                    downloaded = 0
                    chunk_size = 8192  # 8KB chunks
                    
                    for chunk in response.stream(chunk_size):
                        f.write(chunk)
                        downloaded += len(chunk)
                        
                        # 显示进度
                        if downloaded % (1024 * 1024) == 0:  # 每1MB显示一次进度
                            progress = (downloaded / total_size) * 100
                            print(f"下载进度: {progress:.1f}% ({downloaded}/{total_size} 字节)")
                
                response.close()
                response.release_conn()
                
                print(f"文件下载成功: {file_path}")
                return True
                
            except S3Error as e:
                print(f"MinIO S3错误 (尝试 {attempt + 1}): {e}")
                if attempt < max_retries - 1:
                    wait_time = (attempt + 1) * 2  # 递增等待时间
                    print(f"等待 {wait_time} 秒后重试...")
                    time.sleep(wait_time)
                else:
                    print(f"所有重试都失败了")
                    return False
                    
            except Exception as e:
                print(f"下载过程中发生错误 (尝试 {attempt + 1}): {type(e).__name__}: {e}")
                if "IncompleteRead" in str(e) or "Connection broken" in str(e):
                    if attempt < max_retries - 1:
                        wait_time = (attempt + 1) * 2
                        print(f"网络连接中断，等待 {wait_time} 秒后重试...")
                        time.sleep(wait_time)
                        continue
                
                if attempt < max_retries - 1:
                    wait_time = (attempt + 1) * 2
                    print(f"等待 {wait_time} 秒后重试...")
                    time.sleep(wait_time)
                else:
                    import traceback
                    print(f"错误堆栈: {traceback.format_exc()}")
                    return False
        
        return False
    
    def delete_file(self, object_name):
        """
        删除MinIO中的文件
        
        Args:
            object_name (str): MinIO中的对象名称
        
        Returns:
            bool: 删除成功返回True，失败返回False
        """
        try:
            self.client.remove_object(self.bucket_name, object_name)
            print(f"文件删除成功: {object_name}")
            return True
        except S3Error as e:
            print(f"文件删除失败: {e}")
            return False
        except Exception as e:
            print(f"删除过程中发生错误: {e}")
            return False
    
    def list_objects(self, prefix=""):
        """
        列出MinIO中的对象
        
        Args:
            prefix (str): 对象名称前缀
        
        Returns:
            list: 对象列表
        """
        try:
            objects = self.client.list_objects(self.bucket_name, prefix=prefix)
            return [obj.object_name for obj in objects]
        except S3Error as e:
            print(f"列出对象失败: {e}")
            return []
        except Exception as e:
            print(f"列出对象过程中发生错误: {e}")
            return []
    
    def get_presigned_url(self, object_name, expires=timedelta(hours=1)):
        """
        获取预签名URL
        
        Args:
            object_name (str): MinIO中的对象名称
            expires (timedelta): URL过期时间
        
        Returns:
            str: 预签名URL
        """
        try:
            url = self.client.presigned_get_object(self.bucket_name, object_name, expires=expires)
            return url
        except S3Error as e:
            print(f"获取预签名URL失败: {e}")
            return None
        except Exception as e:
            print(f"获取预签名URL过程中发生错误: {e}")
            return None
    
    def upload_model(self, model_path, model_name):
        """
        上传训练好的模型到models文件夹
        
        Args:
            model_path (str): 本地模型文件路径
            model_name (str): 模型名称
        
        Returns:
            str: 上传成功返回MinIO中的文件URL，失败返回None
        """
        object_name = f"models/{model_name}"
        return self.upload_file(model_path, object_name, 'application/octet-stream')
    
    def upload_dataset(self, dataset_path, dataset_name):
        """
        上传数据集到datasets文件夹
        
        Args:
            dataset_path (str): 本地数据集文件路径
            dataset_name (str): 数据集名称
        
        Returns:
            str: 上传成功返回MinIO中的文件URL，失败返回None
        """
        object_name = f"datasets/{dataset_name}"
        return self.upload_file(dataset_path, object_name, 'application/zip')
    
    def upload_prediction_image(self, image_data, image_name):
        """
        上传预测图片到predictions文件夹
        
        Args:
            image_data (bytes): 图片字节数据
            image_name (str): 图片名称
        
        Returns:
            str: 上传成功返回MinIO中的文件URL，失败返回None
        """
        object_name = f"predictions/{image_name}"
        return self.upload_bytes(image_data, object_name)

# 全局MinIO客户端实例
minio_client = MinIOClient()