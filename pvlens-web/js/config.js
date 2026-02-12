// Supabase Configuration
// Replace these with your actual Supabase project credentials
const SUPABASE_URL = 'https://pjbnkhohazorhvyzoqow.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBqYm5raG9oYXpvcmh2eXpvcW93Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzA5MDI0NzEsImV4cCI6MjA4NjQ3ODQ3MX0.Dy-3W1m5DqkN5w4CwQSBCkY5uYVM4MBb0uYz6fkvwNk';

// Initialize Supabase client
const supabase = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// Export for use in other scripts
window.db = supabase;
