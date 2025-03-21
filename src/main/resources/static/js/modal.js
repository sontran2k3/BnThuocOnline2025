class ModalManager {
    constructor() {
        this.init();
    }

    init() {
        $('#addProductModal').on('show.bs.modal', () => this.resetProductModal());
        $('#addInventoryModal').on('show.bs.modal', () => this.loadInventoryModalData());
    }

    resetProductModal() {
        $('#addProductForm')[0].reset();
        $('#image-preview').hide();
    }

    loadInventoryModalData() {
        // Logic tải dữ liệu cho modal thêm lô hàng
    }
}

export default ModalManager;