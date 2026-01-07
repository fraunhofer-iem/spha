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
}
