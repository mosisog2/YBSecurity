/**
 * Advanced Data Source Manager
 * Inspired by Abixen Platform's Business Intelligence Service
 * Supports multiple data sources: CSV, JSON, APIs, Databases
 */

class DataSourceManager {
    constructor() {
        this.dataSources = new Map();
        this.cache = new Map();
        this.mlAnalyzer = new MLDataAnalyzer();
    }

    /**
     * Register a new data source
     */
    registerDataSource(id, config) {
        const dataSource = new DataSource(id, config);
        this.dataSources.set(id, dataSource);
        return dataSource;
    }

    /**
     * Load data from a registered source
     */
    async loadData(sourceId, options = {}) {
        const source = this.dataSources.get(sourceId);
        if (!source) {
            throw new Error(`Data source ${sourceId} not found`);
        }

        const cacheKey = `${sourceId}_${JSON.stringify(options)}`;
        if (this.cache.has(cacheKey) && !options.forceRefresh) {
            return this.cache.get(cacheKey);
        }

        try {
            const data = await source.load(options);
            const analyzedData = await this.mlAnalyzer.analyze(data);
            
            const result = {
                data: data,
                analysis: analyzedData,
                metadata: {
                    source: sourceId,
                    loadTime: new Date().toISOString(),
                    recordCount: data.length,
                    confidence: analyzedData.confidence
                }
            };

            this.cache.set(cacheKey, result);
            return result;
        } catch (error) {
            console.error(`Failed to load data from ${sourceId}:`, error);
            throw error;
        }
    }

    /**
     * Get available data sources
     */
    getAvailableDataSources() {
        return Array.from(this.dataSources.entries()).map(([id, source]) => ({
            id: id,
            name: source.config.name,
            type: source.config.type,
            status: source.status
        }));
    }
}

/**
 * Individual Data Source Class
 */
class DataSource {
    constructor(id, config) {
        this.id = id;
        this.config = config;
        this.status = 'initialized';
        this.lastSync = null;
    }

    async load(options = {}) {
        this.status = 'loading';
        
        try {
            let data;
            
            switch (this.config.type) {
                case 'csv':
                    data = await this.loadCSV();
                    break;
                case 'json':
                    data = await this.loadJSON();
                    break;
                case 'api':
                    data = await this.loadAPI(options);
                    break;
                case 'database':
                    data = await this.loadDatabase(options);
                    break;
                default:
                    throw new Error(`Unsupported data source type: ${this.config.type}`);
            }

            this.status = 'loaded';
            this.lastSync = new Date().toISOString();
            return data;
        } catch (error) {
            this.status = 'error';
            throw error;
        }
    }

    async loadCSV() {
        const response = await fetch(this.config.url);
        const csvText = await response.text();
        return this.parseCSV(csvText);
    }

    async loadJSON() {
        const response = await fetch(this.config.url);
        return await response.json();
    }

    async loadAPI(options) {
        const url = this.buildAPIUrl(options);
        const response = await fetch(url, {
            headers: this.config.headers || {}
        });
        return await response.json();
    }

    async loadDatabase(options) {
        // Simulate database connection (would require backend service)
        const response = await fetch(`/api/database/${this.config.connectionId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                query: this.config.query,
                options: options
            })
        });
        return await response.json();
    }

    parseCSV(csvText) {
        const lines = csvText.trim().split('\n');
        const headers = lines[0].split(',').map(h => h.replace(/"/g, ''));
        
        return lines.slice(1).map(line => {
            const values = this.parseCSVLine(line);
            const record = {};
            headers.forEach((header, index) => {
                record[header] = values[index] || '';
            });
            return record;
        });
    }

    parseCSVLine(line) {
        const values = [];
        let current = '';
        let inQuotes = false;
        
        for (let i = 0; i < line.length; i++) {
            const char = line[i];
            
            if (char === '"') {
                inQuotes = !inQuotes;
            } else if (char === ',' && !inQuotes) {
                values.push(current.replace(/"/g, ''));
                current = '';
            } else {
                current += char;
            }
        }
        
        values.push(current.replace(/"/g, ''));
        return values;
    }

    buildAPIUrl(options) {
        let url = this.config.url;
        if (options.filters) {
            const params = new URLSearchParams(options.filters);
            url += (url.includes('?') ? '&' : '?') + params.toString();
        }
        return url;
    }
}

/**
 * ML Data Analyzer - Enhanced intelligence
 */
class MLDataAnalyzer {
    constructor() {
        this.patterns = new Map();
        this.domainClassifiers = {
            sales: ['sales', 'revenue', 'amount', 'price', 'cost'],
            finance: ['balance', 'profit', 'expense', 'budget', 'investment'],
            hr: ['employee', 'salary', 'department', 'position', 'performance'],
            marketing: ['campaign', 'conversion', 'click', 'impression', 'engagement'],
            operations: ['inventory', 'shipping', 'logistics', 'supply', 'production']
        };
    }

    async analyze(data) {
        if (!data || data.length === 0) {
            return { confidence: 0, domain: 'unknown', insights: [] };
        }

        const analysis = {
            confidence: 0,
            domain: 'unknown',
            columns: [],
            patterns: [],
            recommendations: [],
            quality: {},
            insights: []
        };

        // Analyze columns
        analysis.columns = this.analyzeColumns(data);
        
        // Detect domain
        analysis.domain = this.detectDomain(analysis.columns);
        
        // Find patterns
        analysis.patterns = this.findPatterns(data, analysis.columns);
        
        // Assess data quality
        analysis.quality = this.assessDataQuality(data, analysis.columns);
        
        // Generate insights
        analysis.insights = this.generateInsights(data, analysis);
        
        // Generate recommendations
        analysis.recommendations = this.generateRecommendations(analysis);
        
        // Calculate overall confidence
        analysis.confidence = this.calculateConfidence(analysis);

        return analysis;
    }

    analyzeColumns(data) {
        const sample = data.slice(0, Math.min(100, data.length));
        const columns = Object.keys(sample[0] || {});
        
        return columns.map(column => {
            const values = sample.map(row => row[column]).filter(v => v !== null && v !== undefined && v !== '');
            
            return {
                name: column,
                type: this.detectColumnType(values),
                nullCount: sample.length - values.length,
                uniqueValues: new Set(values).size,
                sampleValues: values.slice(0, 5),
                statistics: this.calculateColumnStatistics(values)
            };
        });
    }

    detectColumnType(values) {
        if (values.length === 0) return 'unknown';
        
        const numericCount = values.filter(v => this.isNumeric(v)).length;
        const dateCount = values.filter(v => this.isDate(v)).length;
        
        if (numericCount / values.length > 0.8) {
            return 'numeric';
        } else if (dateCount / values.length > 0.6) {
            return 'date';
        } else {
            return 'categorical';
        }
    }

    isNumeric(value) {
        if (typeof value === 'number') return true;
        if (typeof value === 'string') {
            // Handle currency and formatted numbers
            const cleaned = value.replace(/[$,\s]/g, '');
            return !isNaN(cleaned) && !isNaN(parseFloat(cleaned));
        }
        return false;
    }

    isDate(value) {
        if (value instanceof Date) return true;
        if (typeof value === 'string') {
            const date = new Date(value);
            return !isNaN(date.getTime());
        }
        return false;
    }

    calculateColumnStatistics(values) {
        const numericValues = values.filter(v => this.isNumeric(v))
                                   .map(v => this.parseNumeric(v));
        
        if (numericValues.length === 0) {
            return { type: 'categorical', distinctCount: new Set(values).size };
        }

        numericValues.sort((a, b) => a - b);
        
        return {
            type: 'numeric',
            min: Math.min(...numericValues),
            max: Math.max(...numericValues),
            mean: numericValues.reduce((a, b) => a + b, 0) / numericValues.length,
            median: numericValues[Math.floor(numericValues.length / 2)],
            std: this.calculateStandardDeviation(numericValues)
        };
    }

    parseNumeric(value) {
        if (typeof value === 'number') return value;
        return parseFloat(value.toString().replace(/[$,\s]/g, ''));
    }

    calculateStandardDeviation(values) {
        const mean = values.reduce((a, b) => a + b, 0) / values.length;
        const variance = values.reduce((acc, val) => acc + Math.pow(val - mean, 2), 0) / values.length;
        return Math.sqrt(variance);
    }

    detectDomain(columns) {
        const columnNames = columns.map(c => c.name.toLowerCase()).join(' ');
        
        let maxScore = 0;
        let detectedDomain = 'general';
        
        for (const [domain, keywords] of Object.entries(this.domainClassifiers)) {
            const score = keywords.reduce((acc, keyword) => {
                return acc + (columnNames.includes(keyword) ? 1 : 0);
            }, 0);
            
            if (score > maxScore) {
                maxScore = score;
                detectedDomain = domain;
            }
        }
        
        return detectedDomain;
    }

    findPatterns(data, columns) {
        const patterns = [];
        
        // Look for trends in numeric columns
        const numericColumns = columns.filter(c => c.type === 'numeric');
        numericColumns.forEach(column => {
            const trend = this.detectTrend(data, column.name);
            if (trend.strength > 0.5) {
                patterns.push({
                    type: 'trend',
                    column: column.name,
                    direction: trend.direction,
                    strength: trend.strength
                });
            }
        });
        
        // Look for correlations
        if (numericColumns.length >= 2) {
            const correlations = this.findCorrelations(data, numericColumns);
            patterns.push(...correlations);
        }
        
        return patterns;
    }

    detectTrend(data, columnName) {
        const values = data.map(row => this.parseNumeric(row[columnName]))
                          .filter(v => !isNaN(v));
        
        if (values.length < 3) return { strength: 0, direction: 'none' };
        
        let increases = 0;
        let decreases = 0;
        
        for (let i = 1; i < values.length; i++) {
            if (values[i] > values[i-1]) increases++;
            else if (values[i] < values[i-1]) decreases++;
        }
        
        const total = increases + decreases;
        if (total === 0) return { strength: 0, direction: 'flat' };
        
        const strength = Math.abs(increases - decreases) / total;
        const direction = increases > decreases ? 'increasing' : 'decreasing';
        
        return { strength, direction };
    }

    findCorrelations(data, numericColumns) {
        const correlations = [];
        
        for (let i = 0; i < numericColumns.length; i++) {
            for (let j = i + 1; j < numericColumns.length; j++) {
                const correlation = this.calculateCorrelation(data, 
                    numericColumns[i].name, numericColumns[j].name);
                
                if (Math.abs(correlation) > 0.6) {
                    correlations.push({
                        type: 'correlation',
                        columns: [numericColumns[i].name, numericColumns[j].name],
                        strength: Math.abs(correlation),
                        direction: correlation > 0 ? 'positive' : 'negative'
                    });
                }
            }
        }
        
        return correlations;
    }

    calculateCorrelation(data, col1, col2) {
        const values1 = data.map(row => this.parseNumeric(row[col1])).filter(v => !isNaN(v));
        const values2 = data.map(row => this.parseNumeric(row[col2])).filter(v => !isNaN(v));
        
        const n = Math.min(values1.length, values2.length);
        if (n < 2) return 0;
        
        const mean1 = values1.reduce((a, b) => a + b, 0) / n;
        const mean2 = values2.reduce((a, b) => a + b, 0) / n;
        
        let numerator = 0;
        let sum1 = 0;
        let sum2 = 0;
        
        for (let i = 0; i < n; i++) {
            const diff1 = values1[i] - mean1;
            const diff2 = values2[i] - mean2;
            numerator += diff1 * diff2;
            sum1 += diff1 * diff1;
            sum2 += diff2 * diff2;
        }
        
        const denominator = Math.sqrt(sum1 * sum2);
        return denominator === 0 ? 0 : numerator / denominator;
    }

    assessDataQuality(data, columns) {
        const totalFields = data.length * columns.length;
        const nullFields = columns.reduce((acc, col) => acc + col.nullCount, 0);
        
        return {
            completeness: ((totalFields - nullFields) / totalFields) * 100,
            consistency: this.assessConsistency(data, columns),
            accuracy: this.assessAccuracy(data, columns),
            uniqueness: this.assessUniqueness(data, columns)
        };
    }

    assessConsistency(data, columns) {
        // Simple consistency check based on data type adherence
        let consistentFields = 0;
        let totalFields = 0;
        
        columns.forEach(column => {
            data.forEach(row => {
                const value = row[column.name];
                totalFields++;
                
                if (value === null || value === undefined || value === '') {
                    return; // Skip null values
                }
                
                let isConsistent = false;
                switch (column.type) {
                    case 'numeric':
                        isConsistent = this.isNumeric(value);
                        break;
                    case 'date':
                        isConsistent = this.isDate(value);
                        break;
                    case 'categorical':
                        isConsistent = typeof value === 'string' || typeof value === 'number';
                        break;
                    default:
                        isConsistent = true;
                }
                
                if (isConsistent) consistentFields++;
            });
        });
        
        return totalFields === 0 ? 100 : (consistentFields / totalFields) * 100;
    }

    assessAccuracy(data, columns) {
        // Simple accuracy assessment - can be enhanced with domain-specific rules
        return 90; // Placeholder
    }

    assessUniqueness(data, columns) {
        const categoricalColumns = columns.filter(c => c.type === 'categorical');
        if (categoricalColumns.length === 0) return 100;
        
        let totalUniqueness = 0;
        categoricalColumns.forEach(column => {
            const uniqueness = (column.uniqueValues / data.length) * 100;
            totalUniqueness += Math.min(uniqueness, 100);
        });
        
        return totalUniqueness / categoricalColumns.length;
    }

    generateInsights(data, analysis) {
        const insights = [];
        
        // Volume insight
        insights.push({
            type: 'volume',
            title: `Dataset contains ${data.length.toLocaleString()} records`,
            description: `Analyzed ${analysis.columns.length} columns with ${analysis.quality.completeness.toFixed(1)}% data completeness`,
            importance: 'medium'
        });
        
        // Domain insight
        insights.push({
            type: 'domain',
            title: `Data domain classified as ${analysis.domain.toUpperCase()}`,
            description: `Machine learning analysis suggests this dataset belongs to the ${analysis.domain} domain`,
            importance: 'high'
        });
        
        // Pattern insights
        analysis.patterns.forEach(pattern => {
            if (pattern.type === 'trend') {
                insights.push({
                    type: 'trend',
                    title: `${pattern.column} shows ${pattern.direction} trend`,
                    description: `Strong ${pattern.direction} pattern detected with ${(pattern.strength * 100).toFixed(1)}% confidence`,
                    importance: 'high'
                });
            } else if (pattern.type === 'correlation') {
                insights.push({
                    type: 'correlation',
                    title: `${pattern.direction} correlation found`,
                    description: `${pattern.columns.join(' and ')} show ${pattern.direction} correlation (strength: ${(pattern.strength * 100).toFixed(1)}%)`,
                    importance: 'medium'
                });
            }
        });
        
        return insights;
    }

    generateRecommendations(analysis) {
        const recommendations = [];
        
        // Data quality recommendations
        if (analysis.quality.completeness < 90) {
            recommendations.push({
                type: 'data-quality',
                title: 'Improve Data Completeness',
                description: 'Consider data cleansing to address missing values',
                priority: 'high'
            });
        }
        
        // Visualization recommendations
        const numericColumns = analysis.columns.filter(c => c.type === 'numeric');
        const categoricalColumns = analysis.columns.filter(c => c.type === 'categorical');
        
        if (numericColumns.length >= 2) {
            recommendations.push({
                type: 'visualization',
                title: 'Create Scatter Plot Analysis',
                description: 'Multiple numeric columns detected - scatter plots could reveal relationships',
                priority: 'medium'
            });
        }
        
        if (categoricalColumns.length >= 1 && numericColumns.length >= 1) {
            recommendations.push({
                type: 'visualization',
                title: 'Use Categorical Grouping',
                description: 'Group numeric data by categories for deeper insights',
                priority: 'medium'
            });
        }
        
        return recommendations;
    }

    calculateConfidence(analysis) {
        let confidence = 0;
        
        // Base confidence on data quality
        confidence += analysis.quality.completeness * 0.3;
        confidence += analysis.quality.consistency * 0.2;
        
        // Boost confidence with patterns found
        confidence += analysis.patterns.length * 10;
        
        // Domain detection confidence
        if (analysis.domain !== 'general') {
            confidence += 20;
        }
        
        return Math.min(confidence, 100);
    }
}

// Export for use in the dashboard
window.DataSourceManager = DataSourceManager;
window.MLDataAnalyzer = MLDataAnalyzer;