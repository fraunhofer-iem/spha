/*
 * Copyright (c) 2026 Fraunhofer IEM. All rights reserved.
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 *
 * SPDX-License-Identifier: MIT
 * License-Filename: LICENSE
 */

import {createApp} from 'vue'
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap/dist/js/bootstrap.bundle.min.js"
import 'bootstrap-icons/font/bootstrap-icons.css';
import './assets/styles/dashboard-card.scss';
import router from './router'
import {store} from "./store.ts";
import {DemoProductRepository} from "./repository/DemoProductRepository.ts";
import {ApiProductRepository} from "./repository/ApiProductRepository.ts";

declare var __DEMO_MODE__: string;

async function main() {

    const App = await import("./App.vue");
    createApp(App.default).use(router).mount('#app')

    // Configure repository based on demo mode
    if (__DEMO_MODE__) {
        console.log('Running in demo mode');
        store.setRepository(new DemoProductRepository());
    } else {
        console.log('Running in API mode');
        store.setRepository(new ApiProductRepository());
    }

    // Load products from the configured repository
    await store.loadProducts();
}

main();
