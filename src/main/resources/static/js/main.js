/**
 * Enhanced Dashboard Alpine.js Component
 * Comprehensive file management functionality
 */

function dashboardApp(initialDashboard) {
    return {
        // UI State
        sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true' || false,
        viewMode: localStorage.getItem('viewMode') || 'grid',
        searchQuery: '',
        selectedFiles: [],

        // Modal States
        showUploadModal: false,
        showCreateFolderModal: false,
        showPasswordModal: false,
        showPasswordVerificationModal: false,
        showShareModal: false,
        showMoveModal: false,
        showPreviewModal: false,

        // Modal triggers (with focus handling)
        openCreateFolderModal() {
            this.showCreateFolderModal = true;
            this.$nextTick(() => document.getElementById('folderName')?.focus());
        },

        openUploadModal() {
            this.showUploadModal = true;
        },

        // Data
        currentFolder: null,
        selectedFile: {
            id: null,
            fileName: ''
        },

        passwordVerificationData: {
            folderId: null,
            folderName: '',
            password: '',
            error: ''
        },

        previewData: {
            fileId: null,
            fileName: '',
            fileType: '',
            fileSize: '',
            previewUrl: '',
            previewType: '', // image, pdf, video, document
            loading: false,
            error: ''
        },

        // Dashboard summary
        metrics: {
            totalFiles: 0,
            totalFolders: 0,
            sharedFiles: 0,
            storageUsed: '0 B',
            storageLimit: '0 B',
            storageUsagePercentage: 0
        },
        recentFiles: [],
        lastUpdated: null,

        // Storage usage
        storageUsed: 0,
        storageTotal: 10, // GB

        // Initialization
        init() {
            this.loadStorageInfo();
            this.setupEventListeners();
            this.setupDragAndDrop();

            // Watch for localStorage changes
            this.$watch('sidebarCollapsed', value => {
                localStorage.setItem('sidebarCollapsed', value);
            });

            this.$watch('viewMode', value => {
                localStorage.setItem('viewMode', value);
            });
        },

        initDashboard() {
            console.log('Dashboard logic initialized');
            // We rely on Thymeleaf for initial render of metrics and storage
            // This object will still hold local state if needed for dynamic updates
            
            // Check for password error from server
            this.checkPasswordError();
        },
        
        checkPasswordError() {
            // Check if there's a password error from form submission
            const urlParams = new URLSearchParams(window.location.search);
            const passwordError = document.querySelector('[data-password-error]')?.dataset.passwordError;
            const errorFolderId = document.querySelector('[data-error-folder-id]')?.dataset.errorFolderId;
            const errorFolderName = document.querySelector('[data-error-folder-name]')?.dataset.errorFolderName;
            
            if (passwordError && errorFolderId) {
                // Reopen password verification modal with error
                this.passwordVerificationData = {
                    folderId: errorFolderId,
                    folderName: errorFolderName || 'Folder',
                    password: '',
                    error: passwordError
                };
                this.showPasswordVerificationModal = true;
                
                this.$nextTick(() => {
                    const input = document.getElementById('verificationPassword');
                    if (input) input.focus();
                });
            }
        },

        get lastUpdatedLabel() {
            if (!this.lastUpdated) return 'No updates yet';
            return `Updated ${this.formatDate(this.lastUpdated)}`;
        },

        // Storage Management
        loadStorageInfo() {
            // This would typically fetch from backend
            // For demo, using mock data
            this.storageUsed = 7.5;
            this.storageTotal = 10;
        },

        get storagePercentage() {
            return (this.storageUsed / this.storageTotal * 100).toFixed(0);
        },

        get storageColor() {
            const percentage = this.storagePercentage;
            if (percentage >= 90) return 'red';
            if (percentage >= 75) return 'amber';
            return 'indigo';
        },

        formatDate(value) {
            if (!value) return '--';
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) return '--';
            return date.toLocaleDateString('vi-VN', {
                day: '2-digit',
                month: 'short',
                year: 'numeric'
            });
        },

        // File Selection
        toggleSelectAll(event) {
            const checkboxes = document.querySelectorAll('.file-checkbox');
            checkboxes.forEach(checkbox => {
                checkbox.checked = event.target.checked;
                const fileId = checkbox.value;

                if (event.target.checked && !this.selectedFiles.includes(fileId)) {
                    this.selectedFiles.push(fileId);
                } else if (!event.target.checked) {
                    this.selectedFiles = this.selectedFiles.filter(id => id !== fileId);
                }
            });
        },

        toggleFileSelection(event) {
            const fileId = event.target.value;

            if (event.target.checked) {
                if (!this.selectedFiles.includes(fileId)) {
                    this.selectedFiles.push(fileId);
                }
            } else {
                this.selectedFiles = this.selectedFiles.filter(id => id !== fileId);
            }
        },

        clearSelection() {
            this.selectedFiles = [];
            document.querySelectorAll('.file-checkbox').forEach(cb => cb.checked = false);
        },

        // Folder Operations
        handleFolderClick(folderId, folderName, hasPassword) {
            console.log('Folder clicked:', folderId, 'Name:', folderName, 'HasPassword:', hasPassword);

            // Convert to boolean correctly
            const isPasswordProtected = hasPassword === true || hasPassword === 'true' || hasPassword === 1 || hasPassword === '1';

            if (isPasswordProtected) {
                this.openPasswordVerification(folderId, folderName || 'Unnamed Folder');
                return;
            }

            // Navigate directly to the folder
            this.navigateToFolder(folderId);
        },

        navigateToFolder(folderId) {
            window.location.href = `/files/dashboard?folderId=${folderId}`;
        },

        openPasswordVerification(folderId, folderName) {
            this.passwordVerificationData = {
                folderId: folderId,
                folderName: folderName,
                password: '',
                error: ''
            };
            this.showPasswordVerificationModal = true;
            this.$nextTick(() => {
                const input = document.getElementById('verificationPassword');
                if (input) input.focus();
            });
        },

        async verifyPassword() {
            try {
                const response = await fetch('/files/folders/verify-password-ajax', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: new URLSearchParams({
                        folderId: this.passwordVerificationData.folderId,
                        password: this.passwordVerificationData.password
                    })
                });

                const payload = await response.json();

                if (payload?.success) {
                    // Store folderId before clearing data
                    const folderId = this.passwordVerificationData.folderId;
                    
                    // Close modal first
                    this.showPasswordVerificationModal = false;
                    // Clear password data
                    this.passwordVerificationData = {
                        folderId: null,
                        folderName: '',
                        password: '',
                        error: ''
                    };
                    // Navigate to folder
                    this.navigateToFolder(folderId);
                } else {
                    this.passwordVerificationData.error = payload?.error || 'Incorrect password. Please try again.';
                    this.passwordVerificationData.password = ''; // Clear password field
                }
            } catch (error) {
                this.passwordVerificationData.error = 'An error occurred. Please try again.';
                this.passwordVerificationData.password = ''; // Clear password field
                console.error('Password verification error:', error);
            }
        },

        // File Upload
        setupDragAndDrop() {
            const dropZone = document.getElementById('dropZone');
            if (!dropZone) return;

            ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
                dropZone.addEventListener(eventName, preventDefaults, false);
            });

            function preventDefaults(e) {
                e.preventDefault();
                e.stopPropagation();
            }

            ['dragenter', 'dragover'].forEach(eventName => {
                dropZone.addEventListener(eventName, () => {
                    dropZone.classList.add('border-indigo-500', 'bg-indigo-50');
                }, false);
            });

            ['dragleave', 'drop'].forEach(eventName => {
                dropZone.addEventListener(eventName, () => {
                    dropZone.classList.remove('border-indigo-500', 'bg-indigo-50');
                }, false);
            });

            dropZone.addEventListener('drop', (e) => {
                const files = e.dataTransfer.files;
                this.handleFiles(files);
            }, false);

            // Click to upload
            dropZone.addEventListener('click', () => {
                document.getElementById('fileInput')?.click();
            });
        },

        handleFiles(files) {
            const fileList = document.getElementById('fileList');
            const filePreview = document.getElementById('filePreview');

            if (!fileList || !filePreview) return;

            filePreview.classList.remove('hidden');
            fileList.innerHTML = '';

            let totalSize = 0;

            Array.from(files).forEach((file, index) => {
                totalSize += file.size;

                const fileItem = this.createFilePreviewItem(file, index);
                fileList.appendChild(fileItem);
            });

            // Update summary
            document.getElementById('totalFiles').textContent = files.length;
            document.getElementById('totalSize').textContent = (totalSize / (1024 * 1024)).toFixed(2);
            document.getElementById('uploadBtn').disabled = false;
        },

        createFilePreviewItem(file, index) {
            const item = document.createElement('div');
            item.className = 'flex items-center justify-between p-3 bg-gray-50 rounded-lg';

            const icon = this.getFileIcon(file.type);
            const size = (file.size / 1024).toFixed(2);

            item.innerHTML = `
                <div class="flex items-center space-x-3 flex-1">
                    <i class="${icon} text-2xl"></i>
                    <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium text-gray-900 truncate">${file.name}</p>
                        <p class="text-xs text-gray-500">${size} KB</p>
                    </div>
                </div>
                <button type="button" onclick="this.parentElement.remove()" class="text-red-500 hover:text-red-700">
                    <i class="fas fa-times"></i>
                </button>
            `;

            return item;
        },

        getFileIcon(mimeType) {
            if (mimeType.startsWith('image/')) return 'fas fa-file-image text-purple-500';
            if (mimeType.startsWith('video/')) return 'fas fa-file-video text-blue-500';
            if (mimeType.startsWith('audio/')) return 'fas fa-file-audio text-green-500';
            if (mimeType.includes('pdf')) return 'fas fa-file-pdf text-red-500';
            if (mimeType.includes('word')) return 'fas fa-file-word text-blue-600';
            if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return 'fas fa-file-excel text-green-600';
            return 'fas fa-file text-gray-500';
        },

        async uploadFiles() {
            const formData = new FormData();
            const fileInput = document.getElementById('fileInput');
            const files = fileInput.files;

            if (files.length === 0) return;

            // Show progress
            const progressBar = document.getElementById('progressBar');
            const progressPercent = document.getElementById('progressPercent');
            const uploadProgress = document.getElementById('uploadProgress');

            uploadProgress.classList.remove('hidden');

            for (let i = 0; i < files.length; i++) {
                formData.append('files', files[i]);
            }

            try {
                const xhr = new XMLHttpRequest();

                xhr.upload.addEventListener('progress', (e) => {
                    if (e.lengthComputable) {
                        const percentComplete = (e.loaded / e.total) * 100;
                        progressBar.style.width = percentComplete + '%';
                        progressPercent.textContent = Math.round(percentComplete) + '%';
                    }
                });

                xhr.addEventListener('load', () => {
                    if (xhr.status === 200) {
                        this.showToast('Files uploaded successfully!', 'success');
                        setTimeout(() => {
                            window.location.reload();
                        }, 1000);
                    } else {
                        this.showToast('Upload failed. Please try again.', 'error');
                    }
                });

                xhr.addEventListener('error', () => {
                    this.showToast('Upload failed. Please try again.', 'error');
                });

                xhr.open('POST', '/files/upload');
                xhr.send(formData);

            } catch (error) {
                console.error('Upload error:', error);
                this.showToast('Upload failed. Please try again.', 'error');
            }
        },

        // File Preview
        async openPreviewModal(fileId, fileName) {
            this.previewData = {
                fileId: fileId,
                fileName: fileName,
                loading: true,
                error: '',
                previewUrl: '',
                previewType: ''
            };

            this.showPreviewModal = true;

            try {
                // Fetch file info
                const response = await fetch(`/files/info/${fileId}`);
                const fileInfo = await response.json();

                this.previewData.fileType = fileInfo.fileType;
                this.previewData.fileSize = this.formatFileSize(fileInfo.fileSize);
                this.previewData.previewUrl = `/files/preview/${fileId}`;

                // Determine preview type
                if (fileInfo.fileType.startsWith('image/')) {
                    this.previewData.previewType = 'image';
                } else if (fileInfo.fileType === 'application/pdf') {
                    this.previewData.previewType = 'pdf';
                } else if (fileInfo.fileType.startsWith('video/')) {
                    this.previewData.previewType = 'video';
                } else {
                    this.previewData.previewType = 'document';
                }

                this.previewData.loading = false;

            } catch (error) {
                this.previewData.loading = false;
                this.previewData.error = 'Failed to load preview';
                console.error('Preview error:', error);
            }
        },

        closePreviewModal() {
            this.showPreviewModal = false;

            // Stop video if playing
            const video = document.getElementById('previewVideoElement');
            if (video) {
                video.pause();
                video.src = '';
            }
        },

        downloadCurrentFile() {
            if (this.previewData.fileId) {
                window.location.href = `/files/download/${this.previewData.fileId}`;
            }
        },

        getFileExtension(fileName) {
            return fileName.split('.').pop().toLowerCase();
        },

        // Bulk Operations
        async downloadSelectedFiles() {
            if (this.selectedFiles.length === 0) {
                this.showToast('Please select files to download', 'warning');
                return;
            }

            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/files/download-multiple';

            this.selectedFiles.forEach(fileId => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'fileIds';
                input.value = fileId;
                form.appendChild(input);
            });

            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);
        },

        async deleteSelectedFiles() {
            if (this.selectedFiles.length === 0) {
                this.showToast('Please select files to delete', 'warning');
                return;
            }

            if (!confirm(`Are you sure you want to delete ${this.selectedFiles.length} file(s)?`)) {
                return;
            }

            try {
                const formData = new URLSearchParams();
                this.selectedFiles.forEach(id => formData.append('id', id));

                const response = await fetch('/files/delete-multiple', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: formData
                });

                if (response.redirected) {
                    window.location.href = response.url;
                } else if (response.ok) {
                    this.showToast('Files deleted successfully', 'success');
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    this.showToast('Failed to delete files', 'error');
                }
            } catch (error) {
                console.error('Delete error:', error);
                this.showToast('Failed to delete files', 'error');
            }
        },

        // Handle folder password button click
        handlePasswordClick(button) {
            const folderId = button.getAttribute('data-folder-id');
            const folderHasPassword = button.getAttribute('data-has-password') === 'true';
            const modalTitle = document.getElementById('passwordModalTitle');
            const modalBtn = document.getElementById('passwordModalBtn');
            const modalHint = document.getElementById('passwordModalHint');
            const folderIdInput = document.getElementById('passwordFolderId');

            if (folderIdInput) folderIdInput.value = folderId;
            if (modalTitle) modalTitle.textContent = folderHasPassword ? 'Update Folder Password' : 'Set Folder Password';
            if (modalBtn) modalBtn.textContent = folderHasPassword ? 'Update' : 'Set Password';
            if (modalHint) modalHint.style.display = folderHasPassword ? 'block' : 'none';

            this.showPasswordModal = true;
        },

        // Share File
        openShareModal(fileId, fileName) {
            this.selectedFile = { id: fileId, fileName: fileName };
            const form = document.getElementById('shareForm');
            if (form) form.action = '/files/send/' + fileId;
            this.showShareModal = true;
        },

        // Move File
        openMoveModal(fileId, fileName) {
            this.selectedFile = { id: fileId, fileName: fileName };
            this.showMoveModal = true;
            document.getElementById('moveFileIdInput').value = fileId;
        },

        async handleMoveSubmit(event) {
            event.preventDefault();
            const form = event.target;
            const fileId = this.selectedFile?.id || form.querySelector('#moveFileIdInput')?.value;
            const targetFolderId = form.querySelector('[name="targetFolderId"]')?.value;

            if (!fileId) {
                this.showToast('File not selected', 'error');
                return;
            }

            try {
                const formData = new URLSearchParams();
                if (targetFolderId) formData.append('targetFolderId', targetFolderId);

                const response = await fetch(`/files/move/${fileId}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: formData
                });

                if (response.redirected) {
                    window.location.href = response.url;
                } else if (response.ok) {
                    this.showToast('File moved successfully', 'success');
                    this.showMoveModal = false;
                    setTimeout(() => window.location.reload(), 1000);
                } else {
                    this.showToast('Failed to move file', 'error');
                }
            } catch (error) {
                console.error('Move error:', error);
                this.showToast('Failed to move file', 'error');
            }
        },

        // Utilities
        togglePassword(inputId) {
            const input = document.getElementById(inputId);
            const icon = document.getElementById(inputId + '-icon');

            if (input && icon) {
                if (input.type === 'password') {
                    input.type = 'text';
                    icon.classList.remove('fa-eye');
                    icon.classList.add('fa-eye-slash');
                } else {
                    input.type = 'password';
                    icon.classList.remove('fa-eye-slash');
                    icon.classList.add('fa-eye');
                }
            }
        },

        formatFileSize(bytes) {
            if (bytes === 0) return '0 Bytes';
            const k = 1024;
            const sizes = ['Bytes', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
        },

        showToast(message, type = 'info') {
            const toast = document.createElement('div');
            toast.className = `toast px-6 py-4 rounded-lg shadow-lg ${type === 'success' ? 'bg-green-500' :
                type === 'error' ? 'bg-red-500' :
                    type === 'warning' ? 'bg-amber-500' :
                        'bg-blue-500'
                } text-white`;
            toast.textContent = message;

            document.body.appendChild(toast);

            setTimeout(() => {
                toast.style.opacity = '0';
                setTimeout(() => toast.remove(), 300);
            }, 3000);
        },

        setupEventListeners() {
            // File input change
            const fileInput = document.getElementById('fileInput');
            if (fileInput) {
                fileInput.addEventListener('change', (e) => {
                    this.handleFiles(e.target.files);
                });
            }

            // Folder password buttons (event delegation - Alpine scope)
            document.addEventListener('click', (e) => {
                const btn = e.target.closest('.folder-password-btn');
                if (btn) {
                    e.stopPropagation();
                    e.preventDefault();
                    this.handlePasswordClick(btn);
                }
            });

            // Share file buttons
            document.querySelectorAll('.share-file-btn').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const fileId = e.currentTarget.dataset.fileId;
                    const fileName = e.currentTarget.dataset.fileName;
                    this.openShareModal(fileId, fileName);
                });
            });

            // Move file buttons
            document.querySelectorAll('.move-file-btn').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const fileId = e.currentTarget.dataset.fileId;
                    const fileName = e.currentTarget.dataset.fileName;
                    this.openMoveModal(fileId, fileName);
                });
            });

            // Preview file buttons
            document.querySelectorAll('.preview-file-btn').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    const fileId = e.currentTarget.dataset.fileId;
                    const fileName = e.currentTarget.dataset.fileName;
                    this.openPreviewModal(fileId, fileName);
                });
            });
        }
    }
}