/**
 * sidebar.js: Collapsible Sidebar Navigation Controller.
 * Supports desktop collapse/expand with localStorage persistence
 * and mobile off-canvas drawer with backdrop overlay.
 */
(function () {
    function initSidebar() {
        var sidebar = document.getElementById('appSidebar');
        if (!sidebar) {
            document.body.classList.remove('has-sidebar', 'sidebar-collapsed');
            return;
        }

        document.body.classList.add('has-sidebar');
        try {
            var savedPref = localStorage.getItem('gt_sidebar_collapsed');
            if (savedPref !== null) {
                if (savedPref === 'true' && window.innerWidth > 900) {
                    document.body.classList.add('sidebar-collapsed');
                } else if (savedPref === 'false') {
                    document.body.classList.remove('sidebar-collapsed');
                }
            }
        } catch (e) {}

        var toggleBtn = document.getElementById('sidebarToggleBtn');
        var backdrop = document.getElementById('sidebarBackdrop');

        function toggleSidebar() {
            if (window.innerWidth <= 900) {
                document.body.classList.toggle('sidebar-mobile-open');
            } else {
                var collapsed = document.body.classList.toggle('sidebar-collapsed');
                try {
                    localStorage.setItem('gt_sidebar_collapsed', collapsed ? 'true' : 'false');
                } catch (e) {}
            }
        }

        if (toggleBtn) {
            toggleBtn.onclick = toggleSidebar;
        }

        if (backdrop) {
            backdrop.onclick = function () {
                document.body.classList.remove('sidebar-mobile-open');
            };
        }

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                document.body.classList.remove('sidebar-mobile-open');
            }
        });

        window.addEventListener('resize', function () {
            if (window.innerWidth > 900) {
                document.body.classList.remove('sidebar-mobile-open');
                try {
                    var isCollapsedNow = localStorage.getItem('gt_sidebar_collapsed') === 'true';
                    if (isCollapsedNow) {
                        document.body.classList.add('sidebar-collapsed');
                    } else if (localStorage.getItem('gt_sidebar_collapsed') === 'false') {
                        document.body.classList.remove('sidebar-collapsed');
                    }
                } catch (e) {}
            } else {
                document.body.classList.remove('sidebar-collapsed');
            }
        });

        // Highlight active link matching current URL path
        var currentPath = window.location.pathname;
        var links = sidebar.querySelectorAll('.sidebar-nav-link');
        links.forEach(function (link) {
            var href = link.getAttribute('href');
            if (!href) return;
            if (href === currentPath || (href !== '/' && currentPath.startsWith(href))) {
                link.classList.add('active');
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSidebar);
    } else {
        initSidebar();
    }
})();
