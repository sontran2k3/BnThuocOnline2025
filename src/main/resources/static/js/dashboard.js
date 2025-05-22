document.addEventListener('DOMContentLoaded', function () {
    // Lấy CSRF token từ meta tag
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    // Hàm định dạng số tiền sang định dạng Việt Nam
    function formatCurrency(value) {
        return value.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
    }

    // --- Tải dữ liệu tổng quan dashboard ---
    fetch('/api/quanly/dashboard/summary', {
        headers: {
            [csrfHeader]: csrfToken
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Lỗi khi lấy dữ liệu tổng quan');
            }
            return response.json();
        })
        .then(data => {
            document.getElementById('total-products').textContent = data.totalProducts || 0;
            document.getElementById('total-orders').textContent = data.totalOrders || 0;
            document.getElementById('total-users').textContent = data.totalUsers || 0;
            document.getElementById('new-reviews').textContent = data.newReviews || 0;
        })
        .catch(error => {
            console.error('Lỗi khi lấy dữ liệu tổng quan:', error);
            Toastify({
                text: "Lỗi khi tải dữ liệu tổng quan!",
                duration: 3000,
                gravity: "top",
                position: "right",
                backgroundColor: "#dc3545"
            }).showToast();
        });

    // --- Tải thống kê đơn hàng ---
    fetch('/api/quanly/dashboard/order-stats', {
        headers: {
            [csrfHeader]: csrfToken
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Lỗi khi lấy dữ liệu đơn hàng');
            }
            return response.json();
        })
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
                        y: {
                            beginAtZero: true,
                            title: { display: true, text: 'Số lượng' }
                        },
                        x: {
                            title: { display: true, text: 'Trạng thái đơn hàng' }
                        }
                    },
                    plugins: {
                        legend: { display: false }
                    }
                }
            });
        })
        .catch(error => {
            console.error('Lỗi khi lấy dữ liệu đơn hàng:', error);
            Toastify({
                text: "Lỗi khi tải dữ liệu đơn hàng!",
                duration: 3000,
                gravity: "top",
                position: "right",
                backgroundColor: "#dc3545"
            }).showToast();
        });

    // --- Tải thống kê sản phẩm theo danh mục ---
    fetch('/api/quanly/dashboard/product-category-stats', {
        headers: {
            [csrfHeader]: csrfToken
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Lỗi khi lấy dữ liệu sản phẩm theo danh mục');
            }
            return response.json();
        })
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
                    plugins: {
                        legend: { position: 'right' }
                    }
                }
            });
        })
        .catch(error => {
            console.error('Lỗi khi lấy dữ liệu sản phẩm theo danh mục:', error);
            Toastify({
                text: "Lỗi khi tải dữ liệu sản phẩm theo danh mục!",
                duration: 3000,
                gravity: "top",
                position: "right",
                backgroundColor: "#dc3545"
            }).showToast();
        });

    // --- Tải biểu đồ doanh thu ---
    let revenueChart = null;

    window.loadRevenueChart = function(type) {
        let apiUrl, labelKey, labelFormat, xAxisTitle;
        switch (type) {
            case 'daily':
                apiUrl = '/api/quanly/dashboard/daily-revenue';
                labelKey = 'ngay';
                labelFormat = (date) => new Date(date).toLocaleDateString('vi-VN');
                xAxisTitle = 'Ngày';
                break;
            case 'weekly':
                apiUrl = '/api/quanly/dashboard/weekly-revenue';
                labelKey = 'bat_dau_tuan';
                labelFormat = (date) => `Tuần bắt đầu ${new Date(date).toLocaleDateString('vi-VN')}`;
                xAxisTitle = 'Tuần';
                break;
            case 'monthly':
                apiUrl = '/api/quanly/dashboard/monthly-revenue';
                labelKey = 'thang_nam';
                labelFormat = (date) => `Tháng ${date.split('-')[1]}/${date.split('-')[0]}`;
                xAxisTitle = 'Tháng';
                break;
            default:
                console.error('Loại biểu đồ không hợp lệ:', type);
                Toastify({
                    text: "Loại biểu đồ không hợp lệ!",
                    duration: 3000,
                    gravity: "top",
                    position: "right",
                    backgroundColor: "#dc3545"
                }).showToast();
                return;
        }

        fetch(apiUrl, {
            headers: {
                [csrfHeader]: csrfToken
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Lỗi khi lấy dữ liệu doanh thu');
                }
                return response.json();
            })
            .then(data => {
                // Chuẩn bị dữ liệu cho biểu đồ
                const labels = data.map(item => labelFormat(item[labelKey]));
                const revenues = data.map(item => parseFloat(item.doanh_thu));

                // Hủy biểu đồ cũ nếu tồn tại
                if (revenueChart) {
                    revenueChart.destroy();
                }

                // Tạo biểu đồ mới
                const revenueChartCtx = document.getElementById('revenueChart').getContext('2d');
                revenueChart = new Chart(revenueChartCtx, {
                    type: 'bar',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Doanh thu',
                            data: revenues,
                            backgroundColor: '#36A2EB',
                            borderColor: '#1E87DB',
                            borderWidth: 1
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Doanh thu (VND)'
                                },
                                ticks: {
                                    callback: function(value) {
                                        return formatCurrency(value);
                                    }
                                }
                            },
                            x: {
                                title: {
                                    display: true,
                                    text: xAxisTitle
                                }
                            }
                        },
                        plugins: {
                            legend: {
                                display: true
                            },
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        let label = context.dataset.label || '';
                                        if (label) {
                                            label += ': ';
                                        }
                                        label += formatCurrency(context.parsed.y);
                                        return label;
                                    }
                                }
                            }
                        }
                    }
                });
            })
            .catch(error => {
                console.error('Lỗi khi tải biểu đồ doanh thu:', error);
                Toastify({
                    text: "Lỗi khi tải dữ liệu doanh thu: " + error.message,
                    duration: 3000,
                    gravity: "top",
                    position: "right",
                    backgroundColor: "#dc3545"
                }).showToast();
            });
    };

    // Tải biểu đồ doanh thu mặc định (theo tháng)
    loadRevenueChart('monthly');
});