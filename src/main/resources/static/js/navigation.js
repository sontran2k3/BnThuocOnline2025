class NavigationManager {
    constructor() {
        this.navLinks = $('.nav-link');
        this.contentSections = $('.content-section');
        this.init();
    }

    init() {
        this.navLinks.on('click', (e) => this.handleNavClick(e));
        this.showSection('dashboard'); // Mặc định hiển thị dashboard
    }

    handleNavClick(e) {
        e.preventDefault();
        const link = $(e.currentTarget);
        this.navLinks.removeClass('active');
        link.addClass('active');
        const sectionId = link.attr('href').substring(1);
        this.showSection(sectionId);
    }

    showSection(sectionId) {
        this.contentSections.hide();
        $(`#${sectionId}`).show();
    }
}

export default NavigationManager;