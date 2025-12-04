import { describe, it, expect } from 'vitest';
import { Product, type Result } from '../../model/Result';

// Mock data
const mockResult1: Result = {
  healthScore: 70,
  repoInfo: {
    version: '1.0.0',
    repoLanguages: [],
    projectUrl: 'https://example.com'
  },
  root: { displayName: 'Root', score: 70, id: 'root', children: [] },
  tools: [],
  createdAt: '2024-01-01'
};

const mockResult2: Result = {
  healthScore: 85,
  repoInfo: {
    version: '1.1.0',
    repoLanguages: [],
    projectUrl: 'https://example.com'
  },
  root: { displayName: 'Root', score: 85, id: 'root', children: [] },
  tools: [],
  createdAt: '2024-01-02'
};

const mockResult3: Result = {
  healthScore: 60,
  repoInfo: {
    version: '1.2.0',
    repoLanguages: [],
    projectUrl: 'https://example.com'
  },
  root: { displayName: 'Root', score: 60, id: 'root', children: [] },
  tools: [],
  createdAt: '2024-01-03'
};

describe('ProductList Health Score Trend Functions', () => {
  it('should return null for products with no results', () => {
    const product = new Product('1', 'Test Product', []);
    
    expect(product.getHealthScoreTrend()).toBe(null);
  });

  it('should return null for products with only one result', () => {
    const product = new Product('1', 'Test Product', [mockResult1]);
    
    expect(product.getHealthScoreTrend()).toBe(null);
  });

  it('should calculate positive trend correctly', () => {
    const product = new Product('1', 'Test Product', [mockResult1, mockResult2]); // 70 -> 85
    
    expect(product.getHealthScoreTrend()).toBe(15);
  });

  it('should calculate negative trend correctly', () => {
    const product = new Product('1', 'Test Product', [mockResult2, mockResult3]); // 85 -> 60
    
    expect(product.getHealthScoreTrend()).toBe(-25);
  });

  it('should return correct color class for positive trend', () => {
    const product = new Product('1', 'Test Product', [mockResult1, mockResult2]); // trend +15
    expect(product.getTrendColorClass()).toBe('text-success');
  });

  it('should return correct color class for negative trend', () => {
    const product = new Product('1', 'Test Product', [mockResult2, mockResult3]); // trend -25
    expect(product.getTrendColorClass()).toBe('text-danger');
  });

  it('should return correct color class for zero trend', () => {
    const product = new Product('1', 'Test Product', [mockResult1, mockResult1]); // trend 0
    expect(product.getTrendColorClass()).toBe('text-muted');
  });

  it('should return correct color class for null trend', () => {
    const product = new Product('1', 'Test Product', [mockResult1]); // only one result
    expect(product.getTrendColorClass()).toBe('text-muted');
  });

  it('should return correct icon for positive trend', () => {
    const product = new Product('1', 'Test Product', [mockResult1, mockResult2]); // trend +15
    expect(product.getTrendIcon()).toBe('bi-arrow-up');
  });

  it('should return correct icon for negative trend', () => {
    const product = new Product('1', 'Test Product', [mockResult2, mockResult3]); // trend -25
    expect(product.getTrendIcon()).toBe('bi-arrow-down');
  });

  it('should return correct icon for zero trend', () => {
    const product = new Product('1', 'Test Product', [mockResult1, mockResult1]); // trend 0
    expect(product.getTrendIcon()).toBe('bi-dash');
  });

  it('should return empty string for null trend', () => {
    const product = new Product('1', 'Test Product', [mockResult1]); // only one result
    expect(product.getTrendIcon()).toBe('');
  });
});