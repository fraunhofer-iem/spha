import { describe, expect, it } from 'vitest';
import { Product, type Result } from '../Result';
import { sortResultsOldestFirst } from '../../repository/ApiProductRepository';

function makeResult(createdAt: string, healthScore: number, version: string): Result {
  return {
    healthScore,
    repoInfo: {
      projectName: 'demo',
      projectUrl: `https://example.org/demo#${version}`,
      version,
      repoLanguages: [{ name: 'Kotlin', size: 100 }],
    },
    root: { displayName: 'root', score: healthScore, id: 'root', children: [] },
    tools: [],
    createdAt,
  };
}

const oldest = makeResult('2026-01-01T09:00:00Z', 40, '1.0.0');
const middle = makeResult('2026-01-02T09:00:00Z', 50, '1.1.0');
const newest = makeResult('2026-01-03T09:00:00Z', 70, '1.2.0');

describe('Product reads the newest result', () => {
  const product = new Product('p1', 'demo', [oldest, middle, newest]);

  it('reports the newest health score', () => {
    expect(product.getCurrentHealthScore()).toBe(70);
  });

  it('reports the newest version', () => {
    expect(product.getNewestVersion()).toBe('1.2.0');
  });

  it('reports the newest project url', () => {
    expect(product.getProjectUrl()).toBe('https://example.org/demo#1.2.0');
  });

  it('returns a positive trend when the newest score improved', () => {
    expect(product.getHealthScoreTrend()).toBe(20);
  });

  it('charts the scores in time order', () => {
    expect(product.getHealthDataForChart().data).toEqual([40, 50, 70]);
  });

  it('gives two results on the same day distinguishable labels', () => {
    const morning = makeResult('2026-02-01T08:30:00Z', 40, '2.0.0');
    const evening = makeResult('2026-02-01T20:45:00Z', 60, '2.0.1');
    const sameDay = new Product('p2', 'demo', [morning, evening]);

    const { labels } = sameDay.getHealthDataForChart();
    expect(labels[0]).not.toBe(labels[1]);
  });
});

describe('sortResultsOldestFirst', () => {
  it('turns the API order (newest first) into the order Product expects', () => {
    const asTheApiReturnsThem = [newest, middle, oldest];

    const product = new Product('p3', 'demo', sortResultsOldestFirst(asTheApiReturnsThem));

    expect(product.getCurrentHealthScore()).toBe(70);
    expect(product.getNewestVersion()).toBe('1.2.0');
    expect(product.getHealthScoreTrend()).toBe(20);
    expect(product.getHealthDataForChart().data).toEqual([40, 50, 70]);
  });
});
