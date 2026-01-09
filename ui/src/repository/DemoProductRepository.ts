/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import type { ProductRepository } from './ProductRepository';
import { Product, type Result } from '../model/Result';
import { parse } from '../util/Parser';

/**
 * Demo repository that returns mock data from remote demo files
 */
export class DemoProductRepository implements ProductRepository {
    private demoUrls: string[] = [
        'https://raw.githubusercontent.com/fraunhofer-iem/spha-ui/refs/heads/main/example/kpi-results.json',
        'https://raw.githubusercontent.com/fraunhofer-iem/spha-ui/refs/heads/main/example/kpi-results-2.json',
        'https://raw.githubusercontent.com/fraunhofer-iem/spha-ui/refs/heads/main/example/kpi-results-small.json'
    ];

    async getAllProducts(): Promise<Product[]> {
        const products: Product[] = [];
        const productMap = new Map<string, Product>();

        try {
            // Fetch all demo data files
            const fetchPromises = this.demoUrls.map(url => this.fetchDemoData(url));
            const results = await Promise.all(fetchPromises);

            // Group results by product name
            for (const result of results) {
                if (result) {
                    const productName = result.repoInfo.projectName || 'Demo Product';
                    const projectUrl = result.repoInfo.projectUrl || '';
                    const productKey = `${productName}-${projectUrl}`;

                    let product = productMap.get(productKey);
                    if (!product) {
                        product = new Product(
                            `demo-${productMap.size + 1}`,
                            productName,
                            [],
                            `Demo analysis results for ${productName}`,
                            result.repoInfo.version,
                            result.createdAt
                        );
                        productMap.set(productKey, product);
                    }

                    product.results.push(result);
                }
            }

            products.push(...productMap.values());
            return products;
        } catch (error) {
            console.error('Error loading demo products:', error);
            return products;
        }
    }

    private async fetchDemoData(url: string): Promise<Result | null> {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                console.error(`Failed to fetch demo data from ${url}`);
                return null;
            }

            const rawData = await response.json();
            const parsedResult = parse(rawData);

            if (!parsedResult) {
                console.error(`Failed to parse demo data from ${url}`);
                return null;
            }

            return parsedResult;
        } catch (error) {
            console.error(`Error fetching demo data from ${url}:`, error);
            return null;
        }
    }
    async getProductByProjectId(_projectId: number): Promise<Product | null> {
        // For demo purposes, we can try to find the product in the already loaded list
        // Since we don't have a persistent ID map in this simple demo repo, we might just return null 
        // or try to match by some logic if needed. 
        // But for the purpose of "updates" in demo mode, usually we don't expect real-time updates.
        // Let's return null to satisfy the interface.
        return null;
    }

    connectToUpdates(_onUpdate: (projectId: number) => void): void {
        // No-op for demo
        console.log('Demo mode: connectToUpdates called (no-op)');
    }

    disconnectFromUpdates(): void {
        // No-op for demo
    }
}
