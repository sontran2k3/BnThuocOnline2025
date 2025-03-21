import SidebarManager from './sidebar.js';
import NavigationManager from './navigation.js';
import ProductManager from './products.js';
import InventoryManager from './inventory.js';
import ModalManager from './modal.js';

$(document).ready(() => {
    new SidebarManager();
    new NavigationManager();
    new ProductManager();
    new InventoryManager();
    new ModalManager();
});