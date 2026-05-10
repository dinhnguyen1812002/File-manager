/**
 * Search functionality for FileHub
 * Powered by Alpine.js
 */

function searchComponent() {
    return {
        query: '',
        suggestions: [],
        selectedIndex: -1,
        showSuggestions: false,

        async fetchSuggestions() {
            if (this.query.length < 2) {
                this.suggestions = [];
                this.showSuggestions = false;
                return;
            }
            try {
                const response = await fetch(`/api/search/suggestions?q=${encodeURIComponent(this.query)}`);
                if (response.ok) {
                    this.suggestions = await response.json();
                    this.showSuggestions = this.suggestions.length > 0;
                    this.selectedIndex = -1;
                }
            } catch (error) {
                console.error('Error fetching suggestions:', error);
            }
        },

        handleKeydown(e) {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                this.selectedIndex = Math.min(this.selectedIndex + 1, this.suggestions.length - 1);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
            } else if (e.key === 'Enter') {
                if (this.selectedIndex >= 0) {
                    e.preventDefault();
                    this.selectSuggestion(this.suggestions[this.selectedIndex]);
                } else if (this.query.trim()) {
                    window.location.href = `/search?q=${encodeURIComponent(this.query)}`;
                }
            } else if (e.key === 'Escape') {
                this.showSuggestions = false;
            }
        },

        selectSuggestion(s) {
            this.query = s.name;
            this.showSuggestions = false;
            window.location.href = `/search?q=${encodeURIComponent(s.name)}`;
        }
    };
}
