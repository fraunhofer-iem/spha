/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import type {Product} from '../model/Result';

/**
 * Repository interface for accessing product data
 */
export interface ProductRepository {
    /**
     * Fetch all products
     * @returns Promise resolving to an array of products
     */
    getAllProducts(): Promise<Product[]>;

    /**
     * Fetch a single product by project ID
     * @param projectId The project ID to fetch
     * @returns Promise resolving to a product or null if not found
     */
    getProductByProjectId(projectId: number): Promise<Product | null>;

    /**
     * Connect to WebSocket for real-time updates
     * @param onUpdate Callback function invoked when a project update is received
     */
    connectToUpdates(onUpdate: (projectId: number) => void): void;

    /**
     * Disconnect from WebSocket
     */
    disconnectFromUpdates(): void;
}
