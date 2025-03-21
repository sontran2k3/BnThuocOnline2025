class SidebarManager {
    constructor() {
        this.sidebar = $('#sidebar');
        this.mainContent = $('#main-content');
        this.toggleButton = $('#toggleSidebar');
        this.navLinks = $('.nav-link');
        this.contentSections = $('.content-section');
        this.init();
    }

    init() {
        this.toggleButton.on('click', () => this.toggleSidebar());
        this.navLinks.on('click', (e) => this.handleNavigation(e));
        this.showSection('dashboard');
    }

    toggleSidebar() {
        this.sidebar.toggleClass('collapsed');
        this.mainContent.toggleClass('expanded');
    }

    showSection(sectionId) {
        this.contentSections.hide();
        $(`#${sectionId}`).show();
    }

    handleNavigation(e) {
        e.preventDefault();
        this.navLinks.removeClass('active');
        const link = $(e.currentTarget);
        link.addClass('active');
        const sectionId = link.attr('href').substring(1);
        this.showSection(sectionId);
    }
}

$(document).ready(() => new SidebarManager());