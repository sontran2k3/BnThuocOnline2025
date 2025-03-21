document.addEventListener('DOMContentLoaded', function () {
    // Gọi API để lấy dữ liệu tổng quan
    fetch('/api/quanly/dashboard/summary')
        .then(response => response.json())
        .then(data => {
            document.getElementById('total-products').textContent = data.totalProducts || 0;
            document.getElementById('total-orders').textContent = data.totalOrders || 0;
            document.getElementById('total-users').textContent = data.totalUsers || 0;
            document.getElementById('new-reviews').textContent = data.newReviews || 0;
        })
        .catch(error => console.error('Lỗi khi lấy dữ liệu tổng quan:', error));

    // Gọi API để lấy thống kê đơn hàng
    fetch('/api/quanly/dashboard/order-stats')
        .then(response => response.json())
        .then(data => {
            const orderChartCtx = document.getElementById('orderChart').getContext('2d');
            new Chart(orderChartCtx, {
                type: 'bar',
                data: {
                    labels: ['Đang chờ', 'Hoàn thành', 'Hủy'],
                    datasets: [{
                        label: 'Số lượng đơn hàng',
                        data: [
                            data.pending || 0,
                            data.completed || 0,
                            data.canceled || 0
                        ],
                        backgroundColor: ['#007bff', '#28a745', '#dc3545'],
                        borderColor: ['#0056b3', '#1e7e34', '#c82333'],
                        borderWidth: 1
                    }]
                },
                options: {
                    scales: {
                        y: { beginAtZero: true, title: { display: true, text: 'Số lượng' } },
                        x: { title: { display: true, text: 'Trạng thái đơn hàng' } }
                    },
                    plugins: { legend: { display: false } }
                }
            });
        })
        .catch(error => console.error('Lỗi khi lấy dữ liệu đơn hàng:', error));

    // Gọi API để lấy thống kê sản phẩm theo danh mục
    fetch('/api/quanly/dashboard/product-category-stats')
        .then(response => response.json())
        .then(data => {
            const productChartCtx = document.getElementById('productChart').getContext('2d');
            new Chart(productChartCtx, {
                type: 'pie',
                data: {
                    labels: Object.keys(data),
                    datasets: [{
                        label: 'Số lượng sản phẩm',
                        data: Object.values(data),
                        backgroundColor: [
                            '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
                            '#FF9F40', '#C9CBCF', '#7BC225', '#F7464A', '#46BFBD',
                            '#FDB45C', '#949FB1'
                        ],
                        hoverOffset: 4
                    }]
                },
                options: {
                    plugins: { legend: { position: 'right' } }
                }
            });
        })
        .catch(error => console.error('Lỗi khi lấy dữ liệu sản phẩm theo danh mục:', error));
});