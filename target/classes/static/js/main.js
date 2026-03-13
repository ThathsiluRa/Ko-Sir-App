/**
 * KO-SIR AI - MAIN JAVASCRIPT
 * =====================================================
 * Handles client-side interactions:
 * - Auto-dismiss flash messages after 5 seconds
 * - Form validation enhancements
 * - Confirmation dialogs for destructive actions
 * - Table search/filter helpers
 * =====================================================
 */

// Run code when DOM is fully loaded
document.addEventListener('DOMContentLoaded', function () {

    // =====================================================
    // AUTO-DISMISS FLASH MESSAGES
    // Success/error alerts automatically fade out after 5 seconds.
    // The user can also dismiss them manually with the X button.
    // =====================================================
    const alerts = document.querySelectorAll('.alert.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            // Bootstrap's dismiss API closes the alert with a fade effect
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) {
                bsAlert.close();
            }
        }, 5000); // 5000ms = 5 seconds
    });

    // =====================================================
    // ACTIVE NAVBAR LINK HIGHLIGHTING
    // Adds "active" class to the current page's nav link.
    // This makes it clear which section you're in.
    // =====================================================
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link');

    navLinks.forEach(function (link) {
        const href = link.getAttribute('href');
        if (href && currentPath.startsWith(href) && href !== '/') {
            link.classList.add('active');
        }
    });

    // =====================================================
    // CONFIRM DANGEROUS ACTIONS
    // Extra protection for delete/destructive forms.
    // This supplements the inline onsubmit="return confirm(...)"
    // for any forms we missed.
    // =====================================================
    const dangerForms = document.querySelectorAll('form[data-confirm]');
    dangerForms.forEach(function (form) {
        form.addEventListener('submit', function (e) {
            const message = form.getAttribute('data-confirm');
            if (!confirm(message)) {
                e.preventDefault(); // Stop the form from submitting
            }
        });
    });

    // =====================================================
    // TABLE SEARCH / FILTER
    // Simple text search for admin tables.
    // If you have an input with id="tableSearch" on a page,
    // it will filter rows in the nearest table.
    // =====================================================
    const tableSearch = document.getElementById('tableSearch');
    if (tableSearch) {
        tableSearch.addEventListener('input', function () {
            const query = this.value.toLowerCase().trim();
            const rows = document.querySelectorAll('tbody tr');

            rows.forEach(function (row) {
                const text = row.textContent.toLowerCase();
                // Show rows that contain the search text
                row.style.display = text.includes(query) ? '' : 'none';
            });
        });
    }

    // =====================================================
    // ROLE SELECTOR (Registration Page)
    // This is defined here as a backup.
    // The primary handler is inline in register.html.
    // =====================================================
    const roleRadios = document.querySelectorAll('input[name="role"]');
    roleRadios.forEach(function (radio) {
        radio.addEventListener('change', function () {
            if (typeof showRoleFields === 'function') {
                showRoleFields(this.value);
            }
        });
    });

    // =====================================================
    // SCROLL REVEAL ANIMATION
    // Elements with class .reveal fade in as you scroll.
    // IntersectionObserver triggers when the element enters
    // the viewport — adds .visible which triggers the CSS transition.
    // =====================================================
    const revealElements = document.querySelectorAll('.reveal');
    if (revealElements.length > 0 && 'IntersectionObserver' in window) {
        const revealObserver = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('visible');
                    revealObserver.unobserve(entry.target); // Only animate once
                }
            });
        }, { threshold: 0.12 });

        revealElements.forEach(function (el) {
            revealObserver.observe(el);
        });
    } else {
        // Fallback: show all immediately if IntersectionObserver not supported
        revealElements.forEach(function (el) { el.classList.add('visible'); });
    }

    // =====================================================
    // OTP INPUT AUTO-FORMAT
    // On the verify page: only allow digits in the OTP field.
    // =====================================================
    const otpInput = document.querySelector('.otp-input');
    if (otpInput) {
        otpInput.addEventListener('input', function () {
            // Strip non-digits and limit to 6 characters
            this.value = this.value.replace(/\D/g, '').slice(0, 6);
        });
    }

    // =====================================================
    // PRICE CALCULATOR BACKUP
    // If durationSelect exists, re-calculate price on change.
    // Primary handler is inline in book.html.
    // =====================================================
    const durationSelect = document.getElementById('durationSelect');
    if (durationSelect) {
        durationSelect.addEventListener('change', function () {
            if (typeof calculatePrice === 'function') {
                calculatePrice();
            }
        });
    }

});
