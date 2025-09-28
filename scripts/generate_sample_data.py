#!/home/student/YBSecurity/lida_env/bin/python3

import json
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import random
import os

def generate_ecommerce_data():
    """Generate sample e-commerce sales data"""
    categories = ['Electronics', 'Books', 'Home & Kitchen', 'Sports', 'Clothing', 'Beauty', 'Toys']
    regions = ['North America', 'Europe', 'Asia', 'South America', 'Africa', 'Oceania']
    channels = ['Online', 'Store', 'Mobile App', 'Third Party']
    
    data = []
    base_date = datetime(2023, 1, 1)
    
    for i in range(2000):
        data.append({
            'order_id': f'ORD-{i+1:06d}',
            'date': base_date + timedelta(days=random.randint(0, 365)),
            'category': random.choice(categories),
            'region': random.choice(regions),
            'channel': random.choice(channels),
            'sales_amount': round(random.uniform(25, 1500), 2),
            'quantity': random.randint(1, 10),
            'discount_percent': round(random.uniform(0, 30), 1),
            'customer_segment': random.choice(['Premium', 'Standard', 'Budget']),
            'profit_margin': round(random.uniform(10, 40), 2)
        })
    
    df = pd.DataFrame(data)
    df.to_csv('datasets/sample_data_ecommerce.csv', index=False)
    print("✅ Generated e-commerce dataset: 2000 records")
    return df

def generate_financial_data():
    """Generate sample financial performance data"""
    departments = ['Sales', 'Marketing', 'Operations', 'R&D', 'HR', 'Finance', 'IT']
    quarters = ['Q1 2023', 'Q2 2023', 'Q3 2023', 'Q4 2023', 'Q1 2024', 'Q2 2024']
    
    data = []
    
    for dept in departments:
        for quarter in quarters:
            base_revenue = random.uniform(500000, 2000000)
            data.append({
                'department': dept,
                'quarter': quarter,
                'revenue': round(base_revenue, 2),
                'expenses': round(base_revenue * random.uniform(0.6, 0.9), 2),
                'profit': round(base_revenue * random.uniform(0.1, 0.4), 2),
                'budget_variance': round(random.uniform(-15, 20), 2),
                'employee_count': random.randint(50, 300),
                'productivity_score': round(random.uniform(70, 95), 1),
                'cost_per_employee': round(random.uniform(80000, 150000), 2)
            })
    
    df = pd.DataFrame(data)
    df.to_csv('datasets/sample_data_financial.csv', index=False)
    print("✅ Generated financial dataset: {} records".format(len(df)))
    return df

def generate_iot_sensor_data():
    """Generate sample IoT sensor data"""
    sensor_types = ['Temperature', 'Humidity', 'Pressure', 'Motion', 'Light', 'Air Quality']
    locations = ['Factory Floor', 'Warehouse', 'Office', 'Parking Lot', 'Server Room', 'Laboratory']
    
    data = []
    base_time = datetime(2024, 1, 1)
    
    for i in range(5000):
        sensor_type = random.choice(sensor_types)
        
        # Generate realistic values based on sensor type
        if sensor_type == 'Temperature':
            value = round(random.uniform(15, 35), 2)
            unit = '°C'
        elif sensor_type == 'Humidity':
            value = round(random.uniform(30, 80), 2)
            unit = '%'
        elif sensor_type == 'Pressure':
            value = round(random.uniform(980, 1020), 2)
            unit = 'hPa'
        elif sensor_type == 'Motion':
            value = random.randint(0, 1)
            unit = 'detected'
        elif sensor_type == 'Light':
            value = round(random.uniform(0, 1000), 2)
            unit = 'lux'
        else:  # Air Quality
            value = round(random.uniform(50, 300), 2)
            unit = 'AQI'
        
        data.append({
            'timestamp': base_time + timedelta(hours=random.randint(0, 8760)),
            'sensor_id': f'{sensor_type[:3].upper()}-{random.randint(100, 999)}',
            'sensor_type': sensor_type,
            'location': random.choice(locations),
            'value': value,
            'unit': unit,
            'status': random.choice(['Online', 'Online', 'Online', 'Offline', 'Maintenance']),
            'battery_level': random.randint(10, 100),
            'signal_strength': random.randint(1, 5)
        })
    
    df = pd.DataFrame(data)
    df.to_csv('datasets/sample_data_iot_sensors.csv', index=False)
    print("✅ Generated IoT sensor dataset: 5000 records")
    return df

def generate_healthcare_data():
    """Generate sample healthcare data"""
    treatments = ['Surgery', 'Medication', 'Physical Therapy', 'Diagnostic Test', 'Consultation', 'Emergency Care']
    departments = ['Cardiology', 'Orthopedics', 'Neurology', 'Pediatrics', 'Emergency', 'Oncology']
    insurance_types = ['Private', 'Medicare', 'Medicaid', 'Self-Pay', 'Workers Comp']
    
    data = []
    
    for i in range(1500):
        age = random.randint(1, 95)
        data.append({
            'patient_id': f'PT-{i+1:06d}',
            'age': age,
            'gender': random.choice(['Male', 'Female']),
            'treatment_type': random.choice(treatments),
            'department': random.choice(departments),
            'admission_date': datetime(2023, 1, 1) + timedelta(days=random.randint(0, 365)),
            'length_of_stay': random.randint(1, 14),
            'treatment_cost': round(random.uniform(500, 25000), 2),
            'insurance_type': random.choice(insurance_types),
            'outcome_score': round(random.uniform(1, 10), 1),
            'readmission': random.choice([0, 0, 0, 1]),  # 25% chance of readmission
            'satisfaction_score': round(random.uniform(6, 10), 1)
        })
    
    df = pd.DataFrame(data)
    df.to_csv('datasets/sample_data_healthcare.csv', index=False)
    print("✅ Generated healthcare dataset: 1500 records")
    return df

def generate_supply_chain_data():
    """Generate sample supply chain data"""
    suppliers = ['Global Supply Co', 'Prime Materials Inc', 'Swift Logistics', 'Quality Parts Ltd', 'Rapid Delivery Corp']
    product_categories = ['Raw Materials', 'Components', 'Packaging', 'Electronics', 'Chemicals']
    countries = ['USA', 'China', 'Germany', 'Japan', 'India', 'Mexico', 'Brazil']
    
    data = []
    
    for i in range(1000):
        data.append({
            'supplier_id': f'SUP-{i+1:04d}',
            'supplier_name': random.choice(suppliers),
            'product_category': random.choice(product_categories),
            'country': random.choice(countries),
            'order_date': datetime(2023, 1, 1) + timedelta(days=random.randint(0, 365)),
            'delivery_date': datetime(2023, 1, 1) + timedelta(days=random.randint(0, 365)),
            'order_value': round(random.uniform(1000, 100000), 2),
            'quality_score': round(random.uniform(70, 100), 1),
            'delivery_performance': round(random.uniform(80, 100), 1),
            'cost_efficiency': round(random.uniform(60, 95), 1),
            'risk_score': round(random.uniform(1, 10), 1),
            'sustainability_rating': random.choice(['A', 'B', 'C', 'D'])
        })
    
    df = pd.DataFrame(data)
    df.to_csv('datasets/sample_data_supply_chain.csv', index=False)
    print("✅ Generated supply chain dataset: 1000 records")
    return df

def generate_query_examples():
    """Generate LIDA query examples for each dataset"""
    examples = {
        'ecommerce': [
            'Show sales trends by category over time',
            'Compare revenue across different regions',
            'Which channel generates the highest profit margins?',
            'Create a correlation analysis between discount and sales amount',
            'What is the distribution of customer segments?'
        ],
        'financial': [
            'Compare department performance by quarter',
            'Show profit trends across all departments',
            'Which department has the best budget variance?',
            'Analyze productivity vs employee count',
            'Create a cost efficiency analysis by department'
        ],
        'iot_sensors': [
            'Show temperature trends by location',
            'Which sensors have the most offline status?',
            'Compare sensor performance across locations',
            'Analyze battery levels by sensor type',
            'Create a signal strength distribution chart'
        ],
        'healthcare': [
            'Compare treatment costs by department',
            'Show patient satisfaction trends by treatment type',
            'Analyze readmission rates by age group',
            'Which insurance type has the highest costs?',
            'Create an outcome score analysis by department'
        ],
        'supply_chain': [
            'Compare supplier performance by country',
            'Show quality scores vs delivery performance',
            'Which product category has the highest risk?',
            'Analyze cost efficiency by supplier',
            'Create a sustainability rating distribution'
        ],
        'retail': [
            'Show sales trends over time',
            'Compare store performance',
            'Which product category performs best?',
            'Analyze customer ratings by category',
            'Create a correlation between economic indicators and sales'
        ]
    }
    
    with open('datasets/lida_query_examples.json', 'w') as f:
        json.dump(examples, f, indent=2)
    
    print("✅ Generated LIDA query examples")
    return examples

def main():
    """Generate all sample datasets"""
    print("🔄 Generating sample datasets for LIDA Multi-Dataset Analytics...")
    
    # Create datasets directory if it doesn't exist
    os.makedirs('datasets', exist_ok=True)
    
    try:
        # Generate all datasets
        generate_ecommerce_data()
        generate_financial_data()
        generate_iot_sensor_data()
        generate_healthcare_data()
        generate_supply_chain_data()
        generate_query_examples()
        
        print("\n🎉 All sample datasets generated successfully!")
        print("📊 Generated datasets:")
        print("  • E-commerce Sales (2000 records)")
        print("  • Financial Performance (42 records)")
        print("  • IoT Sensors (5000 records)")
        print("  • Healthcare (1500 records)")
        print("  • Supply Chain (1000 records)")
        print("  • Query Examples (JSON)")
        
    except Exception as e:
        print(f"❌ Error generating datasets: {e}")

if __name__ == "__main__":
    main()