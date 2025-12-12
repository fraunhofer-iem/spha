import {reactive} from "vue";
import {Product, type Result} from "./model/Result.ts";

// Simple hash function for deterministic ID generation
function generateDeterministicId(name: string, url: string = ""): string {
    const input = `${name}-${url}`;
    let hash = 0;
    for (let i = 0; i < input.length; i++) {
        const char = input.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32bit integer
    }
    return `product-${Math.abs(hash)}`;
}

export const store = reactive({
    products: [] as Product[],
    addResult(result: Result) {
        return addResult(result)
    },
    getProductById(id: string) {
        return this.products.find(product => product.id === id)
    },
    hasProducts() {
        return store.products.length > 0
    }
})


function addResult(result: Result) {
    // Create a new product for this result
    const productName = result.repoInfo.projectName || `Product ${store.products.length + 1}`;
    const projectUrl = result.repoInfo.projectUrl || "";
    const idx = store.products.findIndex(product => product.name === productName);
    let product: Product
    if (idx == -1) {
        product = new Product(
            generateDeterministicId(productName, projectUrl),
            productName,
            [result],
            `Analysis results for ${productName}`,
            result.repoInfo.version,
            new Date().toISOString()
        );

        store.products.push(product);

    } else {
        product = store.products[idx]!!;
        product.results.push(result);
    }

    return product;

}