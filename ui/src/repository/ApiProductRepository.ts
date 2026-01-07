/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import type {ProductRepository} from './ProductRepository';
import {Product, type Result} from '../model/Result';
import {parse} from '../util/Parser';

/**
 * API-based repository that fetches products from the server
 */
export class ApiProductRepository implements ProductRepository {
    private readonly baseUrl: string;

    constructor(baseUrl: string = '') {
        this.baseUrl = baseUrl;
    }

    async getAllProducts(): Promise<Product[]> {
        try {
            // Fetch all project IDs
            const projectsResponse = await fetch(`${this.baseUrl}/api/projects`);
            if (!projectsResponse.ok) {
                throw new Error(`Failed to fetch projects: ${projectsResponse.statusText}`);
            }

            const projectsData = await projectsResponse.json();
            const projectIds = projectsData.projectIds as number[];

            // Fetch results for each project
            const products: Product[] = [];

            for (const projectId of projectIds) {
                const resultsResponse = await fetch(`${this.baseUrl}/api/projects/${projectId}/results`);
                if (!resultsResponse.ok) {
                    console.error(`Failed to fetch results for project ${projectId}`);
                    continue;
                }

                const resultsData = await resultsResponse.json();
                const rawResults = resultsData.results;

                if (!rawResults || rawResults.length === 0) {
                    continue;
                }

                // Parse each raw result using the existing parser
                const parsedResults: Result[] = [];
                for (const rawResult of rawResults) {
                    const parsed = parse(rawResult);
                    if (parsed) {
                        parsedResults.push(parsed);
                    }
                }

                if (parsedResults.length > 0) {
                    // Create a product from the first result
                    const firstResult = parsedResults[0];
                    const productName = firstResult?.repoInfo.projectName || `Product ${projectId}`;

                    const product = new Product(
                        `product-${projectId}`,
                        productName,
                        parsedResults,
                        `Analysis results for ${productName}`,
                        firstResult?.repoInfo.version,
                        firstResult?.createdAt
                    );

                    products.push(product);
                }
            }

            return products;
        } catch (error) {
            console.error('Error fetching products from API:', error);
            throw error;
        }
    }
}
