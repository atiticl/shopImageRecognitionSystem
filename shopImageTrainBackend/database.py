"""
数据库连接和操作模块
"""
import pymysql
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker
from datetime import datetime
import os
from pathlib import Path

class DatabaseManager:
    def __init__(self):
        # 数据库配置
        self.db_config = {
            'host': 'localhost',
            'port': 3306,
            'user': 'root',
            'password': '123456',  # 根据实际情况修改
            'database': 'shop_image',
            'charset': 'utf8mb4'
        }
        
        # 创建数据库连接字符串，添加连接池和超时设置
        self.connection_string = (
            f"mysql+pymysql://{self.db_config['user']}:{self.db_config['password']}"
            f"@{self.db_config['host']}:{self.db_config['port']}/{self.db_config['database']}"
            f"?charset={self.db_config['charset']}"
            f"&connect_timeout=60"
            f"&read_timeout=60"
            f"&write_timeout=60"
        )
        
        self.engine = None
        self.Session = None
        
    def get_connection(self):
        """获取数据库连接"""
        try:
            connection = pymysql.connect(
                host=self.db_config['host'],
                port=self.db_config['port'],
                user=self.db_config['user'],
                password=self.db_config['password'],
                database=self.db_config['database'],
                charset='utf8mb4',
                cursorclass=pymysql.cursors.DictCursor,
                connect_timeout=60,
                read_timeout=60,
                write_timeout=60,
                autocommit=True
            )
            return connection
        except Exception as e:
            print(f"数据库连接失败: {e}")
            return None
    
    def connect(self):
        """建立数据库连接"""
        try:
            # 添加连接池配置
            self.engine = create_engine(
                self.connection_string, 
                echo=False,
                pool_size=10,
                max_overflow=20,
                pool_pre_ping=True,
                pool_recycle=3600
            )
            self.Session = sessionmaker(bind=self.engine)
            print("数据库连接成功")
            return True
        except Exception as e:
            print(f"数据库连接失败: {e}")
            return False
    
    def insert_model(self, model_info):
        """
        插入模型信息到数据库
        
        Args:
            model_info (dict): 模型信息字典，包含以下字段：
                - name: 模型名称
                - version: 版本号
                - file_url: 文件路径
                - framework: 框架类型 (PYTORCH/TENSORFLOW)
                - accuracy: 准确率
                - file_size: 文件大小
                - training_samples: 训练样本数
                - precision_score: 精确率
                - recall_score: 召回率
                - f1_score: F1分数
                - description: 描述
                - type: 模型类型 (GENERAL/CLOTHING/ELECTRONICS)
        """
        if not self.engine:
            if not self.connect():
                return False
                
        try:
            session = self.Session()
            
            # 构建插入SQL
            insert_sql = text("""
                INSERT INTO model (
                    name, version, file_url, framework, accuracy, 
                    file_size, training_samples, precision_score, 
                    recall_score, f1_score, description, type, status
                ) VALUES (
                    :name, :version, :file_url, :framework, :accuracy,
                    :file_size, :training_samples, :precision_score,
                    :recall_score, :f1_score, :description, :type, :status
                )
            """)
            
            # 执行插入
            result = session.execute(insert_sql, {
                'name': model_info.get('name'),
                'version': model_info.get('version', '1.0.0'),
                'file_url': model_info.get('file_url'),
                'framework': model_info.get('framework', 'PYTORCH'),
                'accuracy': model_info.get('accuracy'),
                'file_size': model_info.get('file_size'),
                'training_samples': model_info.get('training_samples'),
                'precision_score': model_info.get('precision_score'),
                'recall_score': model_info.get('recall_score'),
                'f1_score': model_info.get('f1_score'),
                'description': model_info.get('description'),
                'type': model_info.get('type', 'GENERAL'),
                'status': 'INACTIVE'  # 新训练的模型默认为非激活状态
            })
            
            session.commit()
            model_id = result.lastrowid
            session.close()
            
            print(f"模型信息已成功插入数据库，ID: {model_id}")
            return model_id
            
        except Exception as e:
            print(f"插入模型信息失败: {e}")
            if session:
                session.rollback()
                session.close()
            return False
    
    def get_model_by_name(self, name):
        """根据名称查询模型"""
        if not self.engine:
            if not self.connect():
                return None
                
        try:
            session = self.Session()
            
            query_sql = text("SELECT * FROM model WHERE name = :name")
            result = session.execute(query_sql, {'name': name})
            model = result.fetchone()
            
            session.close()
            return model
            
        except Exception as e:
            print(f"查询模型失败: {e}")
            return None
    
    def get_all_models(self):
        """获取所有模型，按创建时间降序排列"""
        if not self.engine:
            if not self.connect():
                return []
                
        try:
            session = self.Session()
            
            # 使用正确的字段名查询
            query_sql = text("SELECT * FROM model ORDER BY created_at DESC")
            result = session.execute(query_sql)
            
            models = []
            
            for row in result:
                # 将Row对象转换为字典，使用正确的字段名
                model_dict = {
                    'id': row.id,
                    'model_name': row.name,
                    'version': row.version,
                    'file_url': row.file_url,
                    'framework': row.framework,
                    'accuracy': row.accuracy,
                    'file_size': row.file_size,
                    'training_samples': row.training_samples,
                    'precision_score': row.precision_score,
                    'recall_score': row.recall_score,
                    'f1_score': row.f1_score,
                    'description': row.description,
                    'type': row.type,
                    'status': row.status,
                    'created_time': row.created_at,  # 使用正确的字段名
                    'updated_time': row.updated_at   # 使用正确的字段名
                }
                models.append(model_dict)
            
            session.close()
            return models
            
        except Exception as e:
            print(f"查询所有模型失败: {e}")
            return []
    
    def update_model_status(self, model_id, status):
        """更新模型状态"""
        if not self.engine:
            if not self.connect():
                return False
                
        try:
            session = self.Session()
            
            update_sql = text("UPDATE model SET status = :status WHERE id = :id")
            session.execute(update_sql, {'status': status, 'id': model_id})
            session.commit()
            session.close()
            
            print(f"模型状态已更新: ID={model_id}, Status={status}")
            return True
            
        except Exception as e:
            print(f"更新模型状态失败: {e}")
            return False

# 全局数据库管理器实例
db_manager = DatabaseManager()