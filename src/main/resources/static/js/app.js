/**
 * Unified Dashboard Application
 * Combines all dashboard functionality into one optimized file
 */

function dashboardApp(initialDashboard) {
    const dashboard = initialDashboard || {};
    
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

        // Data Objects
        selectedFile: { id: null, fileName: '' },
        passwordVerificationData: { folderId: null, folderName: '', password: '', error: '' },
        previewData: { fileId: null, fileName: '', fileType: '', fileSize: '', previewUrl: '', previewType: '', loading: false, error: '' },

        // Initialization
        init() {
            this.setupEventListeners();
            this.setupDragAndDrop();
            this.$watch('sidebarCollapsed', value => localStorage.setItem('sidebarCollapsed', value));
            this.$watch('viewMode', value => localStorage.setItem('viewMode', value));
        },

        initDashboard() {
            console.log('Dashboard initialized');
            this.checkPasswordError();
        },

        // Modal Controls
        openCreateFolderModal() {
            this.showCreateFolderModal = true;
            this.$nextTick(() => document.getElementById('folderName')?.focus());
        },

        openUploadModal() {
            this.showUploadModal = true;
        },

        // Password Error Handling
        checkPasswordError() {
            const passwordError = document.querySelector('[data-password-error]')?.dataset.passwordError;
            const errorFolderId = document.querySelector('[data-error-folder-id]')?.dataset.errorFolderId;
            const errorFolderName = document.querySelector('[data-error-folder-name]')?.dataset.errorFolderName;
            
            if (passwordError && errorFolderId) {
                this.passwordVerificationData = {
                    folderId: errorFolderId,
                    folderName: errorFolderName || 'Folder',
                    password: '',
                    error: passwordError
                };
                this.showPasswordVerificationModal = true;
                this.$nextTick(() => document.getElementById('verificationPassword')?.focus());
            }
        },

        // Folder Operations
        handleFolderClick(folderId, folderName, hasPassword) {
            const isPasswordProtected = hasPassword === true || hasPassword === 'true';
            if (isPasswordProtected) {
                this.openPasswordVerification(folderId, folderName || 'Unnamed Folder');
            } else {
                this.navigateToFolder(folderId);
            }
        },

        navigateToFolder(folderId) {
            window.location.href = `/files/dashboard?folderId=${folderId}`;
        },

        openPasswordVerification(folderId, folderName) {
            this.passwordVerificationData = { folderId, folderName, password: '', error: '' };
            this.showPasswordVerificationModal = true;
            this.$nextTick(() => {
                const hiddenInput = document.querySelector('input[name="folderId"]');
                if (hiddenInput) hiddenInput.value = folderId;
                document.getElementById('verificationPassword')?.focus();
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
                if (!this.selectedFiles.includes(fileId)) this.selectedFiles.push(fileId);
            } else {
                this.selectedFiles = this.selectedFiles.filter(id => id !== fileId);
            }
        },

        // File Operations
        async openPreviewModal(fileId, fileName) {
            this.previewData = { fileId, fileName, loading: true, error: '', previewUrl: '', previewType: '' };
            this.showPreviewModal = true;

            try {
                const [metadataResponse, previewResponse] = await Promise.all([
                    fetch(`/files/api/preview/${fileId}/metadata`),
                    fetch(`/files/api/preview/${fileId}/info`)
                ]);

                if (!metadataResponse.ok || !previewResponse.ok) {
                    throw new Error('Failed to load file data');
                }

                const [metadata, previewInfo] = await Promise.all([
                    metadataResponse.json(),
                    previewResponse.json()
                ]);

                this.previewData = {
                    ...this.previewData,
                    fileName: metadata.fileName,
                    fileType: metadata.fileType,
                    fileSize: metadata.fileSizeFormatted,
                    previewUrl: previewInfo.previewUrl,
                    previewType: previewInfo.previewType.toLowerCase(),
                    loading: false
                };
            } catch (error) {
                this.previewData.error = error.message;
                this.previewData.loading = false;
            }
        },

        closePreviewModal() {
            // Stop video playback
            const videos = document.querySelectorAll('.preview-modal video');
            videos.forEach(video => {
                if (!video.paused) video.pause();
                video.src = '';
                video.load();
            });

            this.showPreviewModal = false;
            this.previewData = { fileId: null, fileName: '', fileType: '', fileSize: '', previewUrl: '', previewType: '', loading: false, error: '' };
        },

        downloadCurrentFile() {
            if (this.previewData.fileId) {
                window.location.href = `/files/download/${this.previewData.fileId}`;
            }
        },

        // Share & Move Operations
        openShareModal(fileId, fileName) {
            this.selectedFile = { id: fileId, fileName };
            const form = document.getElementById('shareForm');
            if (form) form.action = '/files/send/' + fileId;
            this.showShareModal = true;
        },

        openMoveModal(fileId, fileName) {
            this.selectedFile = { id: fileId, fileName };
            this.showMoveModal = true;
            const input = document.getElementById('moveFileIdInput');
            if (input) input.value = fileId;
        },

        // Bulk Operations
        async deleteSelectedFiles() {
            if (this.selectedFiles.length === 0) {
                this.showToast('Please select files to delete', 'warning');
                return;
            }

            if (!confirm(`Delete ${this.selectedFiles.length} file(s)?`)) return;

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
                this.showToast('Failed to delete files', 'error');
            }
        },

        // Drag & Drop Setup
        setupDragAndDrop() {
            const dropZone = document.getElementById('dropZone');
            if (!dropZone) return;

            ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
                dropZone.addEventListener(eventName, e => { e.preventDefault(); e.stopPropagation(); });
            });

            ['dragenter', 'dragover'].forEach(eventName => {
                dropZone.addEventListener(eventName, () => dropZone.classList.add('border-green-500', 'bg-green-50'));
            });

            ['dragleave', 'drop'].forEach(eventName => {
                dropZone.addEventListener(eventName, () => dropZone.classList.remove('border-green-500', 'bg-green-50'));
            });

            dropZone.addEventListener('drop', e => this.handleFiles(e.dataTransfer.files));
            dropZone.addEventListener('click', () => document.getElementById('fileInput')?.click());
        },

        // File Handling
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

        // Event Listeners Setup
        setupEventListeners() {
            const fileInput = document.getElementById('fileInput');
            if (fileInput) {
                fileInput.addEventListener('change', e => this.handleFiles(e.target.files));
            }

            // Event delegation for dynamic buttons
            document.addEventListener('click', e => {
                const btn = e.target.closest('.folder-password-btn');
                if (btn) {
                    e.stopPropagation();
                    e.preventDefault();
                    this.handlePasswordClick(btn);
                }

                if (e.target.closest('.share-file-btn')) {
                    const shareBtn = e.target.closest('.share-file-btn');
                    this.openShareModal(shareBtn.dataset.fileId, shareBtn.dataset.fileName);
                }

                if (e.target.closest('.move-file-btn')) {
                    const moveBtn = e.target.closest('.move-file-btn');
                    this.openMoveModal(moveBtn.dataset.fileId, moveBtn.dataset.fileName);
                }

                if (e.target.closest('.preview-file-btn')) {
                    const previewBtn = e.target.closest('.preview-file-btn');
                    this.openPreviewModal(previewBtn.dataset.fileId, previewBtn.dataset.fileName);
                }
            });
        },

        // Utility Methods
        handlePasswordClick(button) {
            const folderId = button.getAttribute('data-folder-id');
            const folderHasPassword = button.getAttribute('data-has-password') === 'true';
            
            const folderIdInput = document.getElementById('passwordFolderId');
            const modalTitle = document.getElementById('passwordModalTitle');
            const modalBtn = document.getElementById('passwordModalBtn');
            const modalHint = document.getElementById('passwordModalHint');

            if (folderIdInput) folderIdInput.value = folderId;
            if (modalTitle) modalTitle.textContent = folderHasPassword ? 'Update Folder Password' : 'Set Folder Password';
            if (modalBtn) modalBtn.textContent = folderHasPassword ? 'Update' : 'Set Password';
            if (modalHint) modalHint.style.display = folderHasPassword ? 'block' : 'none';

            this.showPasswordModal = true;
        },

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

        getFileIcon(mimeType) {
            if (mimeType.startsWith('image/')) return 'fas fa-file-image text-purple-500';
            if (mimeType.startsWith('video/')) return 'fas fa-file-video text-blue-500';
            if (mimeType.startsWith('audio/')) return 'fas fa-file-audio text-green-500';
            if (mimeType.includes('pdf')) return 'fas fa-file-pdf text-red-500';
            if (mimeType.includes('word')) return 'fas fa-file-word text-blue-600';
            if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return 'fas fa-file-excel text-green-600';
            return 'fas fa-file text-gray-500';
        },

        getFileExtension(fileName) {
            if (!fileName) return 'mp4';
            const parts = fileName.split('.');
            if (parts.length < 2) return 'mp4';
            const ext = parts.pop().toLowerCase();
            const extensionMap = {
                'mp4': 'mp4', 'webm': 'webm', 'ogg': 'ogg', 'ogv': 'ogg',
                'avi': 'x-msvideo', 'mov': 'quicktime', 'mkv': 'x-matroska'
            };
            return extensionMap[ext] || 'mp4';
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
            toast.className = `fixed top-4 right-4 px-6 py-4 rounded-lg shadow-lg z-50 ${
                type === 'success' ? 'bg-green-500' :
                type === 'error' ? 'bg-red-500' :
                type === 'warning' ? 'bg-amber-500' : 'bg-blue-500'
            } text-white`;
            toast.textContent = message;
            document.body.appendChild(toast);
            setTimeout(() => {
                toast.style.opacity = '0';
                setTimeout(() => toast.remove(), 300);
            }, 3000);
        }
    }
}

// Video Player Component (Simplified)
function videoPlayer() {
    return {
        isPlaying: false,
        isMuted: false,
        volume: 100,
        currentTime: 0,
        duration: 0,
        showControls: false,
        videoElement: null,

        init() {
            this.$nextTick(() => {
                this.videoElement = this.$refs.videoElement;
                if (this.videoElement) this.setupVideoEvents();
            });
        },

        setupVideoEvents() {
            document.addEventListener('keydown', (e) => {
                if (!this.showControls) return;
                switch(e.code) {
                    case 'Space': e.preventDefault(); this.togglePlayPause(); break;
                    case 'KeyM': e.preventDefault(); this.toggleMute(); break;
                    case 'KeyF': e.preventDefault(); this.toggleFullscreen(); break;
                }
            });
        },

        togglePlayPause() {
            if (!this.videoElement) return;
            if (this.videoElement.paused) {
                this.videoElement.play();
                this.isPlaying = true;
            } else {
                this.videoElement.pause();
                this.isPlaying = false;
            }
        },

        toggleMute() {
            if (!this.videoElement) return;
            this.videoElement.muted = !this.videoElement.muted;
            this.isMuted = this.videoElement.muted;
        },

        toggleFullscreen() {
            const container = this.videoElement.parentElement;
            if (!document.fullscreenElement) {
                container.requestFullscreen?.();
            } else {
                document.exitFullscreen?.();
            }
        },

        formatTime(seconds) {
            if (!seconds || isNaN(seconds)) return '0:00';
            const minutes = Math.floor(seconds / 60);
            const remainingSeconds = Math.floor(seconds % 60);
            return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
        }
    };
}