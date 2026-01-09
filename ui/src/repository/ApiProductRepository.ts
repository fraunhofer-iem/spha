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
 * API-based repository that fetches products from the server
 */
export class ApiProductRepository implements ProductRepository {
    private readonly baseUrl: string;
    private websocket: WebSocket | null = null;
    private onUpdateCallback: ((projectId: number) => void) | null = null;

    private readonly reconnectInterval = 3000; // Start with 3 seconds
    private readonly maxReconnectAttempts = 20;
    private reconnectAttempts = 0;

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

    async getProductByProjectId(projectId: number): Promise<Product | null> {
        try {
            const resultsResponse = await fetch(`${this.baseUrl}/api/projects/${projectId}/results`);
            if (!resultsResponse.ok) {
                console.error(`Failed to fetch results for project ${projectId}`);
                return null;
            }

            const resultsData = await resultsResponse.json();
            const rawResults = resultsData.results;

            if (!rawResults || rawResults.length === 0) {
                return null;
            }

            // Parse each raw result using the existing parser
            const parsedResults: Result[] = [];
            for (const rawResult of rawResults) {
                const parsed = parse(rawResult);
                if (parsed) {
                    parsedResults.push(parsed);
                }
            }

            if (parsedResults.length === 0) {
                return null;
            }

            // Create a product from the first result
            const firstResult = parsedResults[0];
            const productName = firstResult?.repoInfo.projectName || `Product ${projectId}`;

            return new Product(
                `product-${projectId}`,
                productName,
                parsedResults,
                `Analysis results for ${productName}`,
                firstResult?.repoInfo.version,
                firstResult?.createdAt
            );
        } catch (error) {
            console.error(`Error fetching product for project ${projectId}:`, error);
            return null;
        }
    }

    connectToUpdates(onUpdate: (projectId: number) => void): void {
        this.onUpdateCallback = onUpdate;
        this.connect();
    }

    private connect(): void {
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }

        const wsUrl = this.constructWebSocketUrl();
        console.log(`Connecting to WebSocket at ${wsUrl}`);

        try {
            this.websocket = new WebSocket(wsUrl);

            this.websocket.onopen = () => {
                console.log('WebSocket connected');
                this.reconnectAttempts = 0; // Reset attempts on successful connection
            };

            this.websocket.onmessage = (event) => {
                try {
                    const projectId = parseInt(event.data, 10);
                    if (!isNaN(projectId) && this.onUpdateCallback) {
                        this.onUpdateCallback(projectId);
                    }
                } catch (error) {
                    console.error('Error parsing WebSocket message:', error);
                }
            };

            this.websocket.onerror = (error) => {
                console.error('WebSocket error:', error);
            };

            this.websocket.onclose = () => {
                console.log('WebSocket disconnected');
                this.handleReconnection();
            };
        } catch (error) {
            console.error('Failed to create WebSocket connection:', error);
            this.handleReconnection();
        }
    }

    private constructWebSocketUrl(): string {
        let url = this.baseUrl;

        // If baseUrl is empty, use current window location
        if (!url) {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            return `${protocol}//${window.location.host}/api/ws/updates`;
        }

        // If baseUrl starts with http/https, replace it
        if (url.startsWith('http://')) {
            url = url.replace('http://', 'ws://');
        } else if (url.startsWith('https://')) {
            url = url.replace('https://', 'wss://');
        } else if (url.startsWith('//')) {
            // Handle protocol-relative URLs
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            url = `${protocol}${url}`;
        } else if (!url.startsWith('ws://') && !url.startsWith('wss://')) {
            // Assume relative path or missing protocol, prepend appropriate ws protocol + host if needed
            // But if it's just a path like "/api", we need to know the host. 
            // If baseUrl was just a path, strictly speaking fetching wouldn't work easily without a host unless it's relative.
            // Let's assume if it doesn't start with a protocol, it's relative to current window or needs protocol inference.

            // Safer approach: define protocol based on current page if not present
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            if (url.startsWith('/')) {
                url = `${protocol}//${window.location.host}${url}`;
            } else {
                // It might be a full host without protocol, unlikely but possible. 
                // Or it's "localhost:8080".
                url = `${protocol}//${url}`;
            }
        }

        // Ensure proper suffix
        // If the base url already includes /api, we might need to adjust. 
        // The original code appended '/api/ws/updates'. 
        // Let's assume baseUrl is the root of the backend (e.g. http://localhost:8080).

        // Remove trailing slash if present to avoid double slashes
        if (url.endsWith('/')) {
            url = url.slice(0, -1);
        }

        return `${url}/api/ws/updates`;
    }

    private handleReconnection(): void {
        if (!this.onUpdateCallback) {
            // If disconnected intentionally (callback null), don't reconnect
            return;
        }

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = this.reconnectInterval * Math.pow(1.5, this.reconnectAttempts - 1); // Exponential backoff
            console.log(`Scheduling reconnect attempt ${this.reconnectAttempts} in ${delay}ms`);

            setTimeout(() => {
                if (this.onUpdateCallback) { // Check again in case it was stopped in the meantime
                    this.connect();
                }
            }, delay);
        } else {
            console.error('Max WebSocket reconnection attempts reached');
        }
    }

    disconnectFromUpdates(): void {
        this.onUpdateCallback = null; // prevents reconnection
        if (this.websocket) {
            this.websocket.close();
            this.websocket = null;
        }
    }
}
