/**
 * File List functionality for FileHub
 */

document.addEventListener('DOMContentLoaded', function () {
    // Tab Switching
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    if (tabBtns.length > 0) {
        tabBtns.forEach(btn => {
            btn.addEventListener('click', function () {
                const targetTab = this.dataset.tab;

                // Update tab buttons
                tabBtns.forEach(b => {
                    b.classList.remove('tab-active');
                    b.classList.add('bg-white', 'text-gray-600', 'shadow-sm');
                });
                this.classList.add('tab-active');
                this.classList.remove('bg-white', 'text-gray-600', 'shadow-sm');

                // Update tab contents
                tabContents.forEach(content => {
                    content.classList.add('hidden');
                    if (content.id === targetTab) {
                        content.classList.remove('hidden');
                    }
                });
            });
        });
    }

    // View Toggle
    const gridViewBtn = document.getElementById('gridView');
    const listViewBtn = document.getElementById('listView');
    const uploadedGrid = document.getElementById('uploadedGrid');
    const uploadedList = document.getElementById('uploadedList');
    const sharedGrid = document.getElementById('sharedGrid');
    const sharedList = document.getElementById('sharedList');

    if (gridViewBtn && listViewBtn) {
        gridViewBtn.addEventListener('click', function () {
            this.classList.add('bg-white', 'shadow-sm', 'text-green-600');
            this.classList.remove('text-gray-500');
            listViewBtn.classList.remove('bg-white', 'shadow-sm', 'text-green-600');
            listViewBtn.classList.add('text-gray-500');

            uploadedGrid?.classList.remove('hidden');
            uploadedList?.classList.add('hidden');
            sharedGrid?.classList.remove('hidden');
            sharedList?.classList.add('hidden');
        });

        listViewBtn.addEventListener('click', function () {
            this.classList.add('bg-white', 'shadow-sm', 'text-green-600');
            this.classList.remove('text-gray-500');
            gridViewBtn.classList.remove('bg-white', 'shadow-sm', 'text-green-600');
            gridViewBtn.classList.add('text-gray-500');

            uploadedGrid?.classList.add('hidden');
            uploadedList?.classList.remove('hidden');
            sharedGrid?.classList.add('hidden');
            sharedList?.classList.remove('hidden');
        });
    }

    // Search Functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('input', function () {
            const searchTerm = this.value.toLowerCase();
            const fileItems = document.querySelectorAll('.file-item');

            fileItems.forEach(item => {
                const fileName = item.dataset.filename?.toLowerCase() || '';
                if (fileName.includes(searchTerm)) {
                    item.style.display = '';
                } else {
                    item.style.display = 'none';
                }
            });
        });
    }
});
