/**
 * MY Manus Design System
 * Professional Minimalist Theme
 * 
 * This design system provides a cohesive, modern aesthetic across the entire application.
 * Inspired by: Linear, Vercel, GitHub's modern UI
 */

export const theme = {
  /**
   * Color Palette
   * Carefully selected for accessibility and visual hierarchy
   */
  colors: {
    // Primary Brand Colors
    primary: {
      50: '#eff6ff',
      100: '#dbeafe',
      200: '#bfdbfe',
      300: '#93c5fd',
      400: '#60a5fa',
      500: '#3b82f6',  // Main primary
      600: '#2563eb',
      700: '#1d4ed8',
      800: '#1e40af',
      900: '#1e3a8a',
    },

    // Semantic Colors
    success: {
      light: '#86efac',
      DEFAULT: '#22c55e',
      dark: '#16a34a',
    },
    
    warning: {
      light: '#fcd34d',
      DEFAULT: '#f59e0b',
      dark: '#d97706',
    },
    
    error: {
      light: '#fca5a5',
      DEFAULT: '#ef4444',
      dark: '#dc2626',
    },
    
    info: {
      light: '#7dd3fc',
      DEFAULT: '#0ea5e9',
      dark: '#0284c7',
    },

    // Neutral Grays (Dark Theme Optimized)
    gray: {
      50: '#f8fafc',
      100: '#f1f5f9',
      200: '#e2e8f0',
      300: '#cbd5e1',
      400: '#94a3b8',
      500: '#64748b',
      600: '#475569',
      700: '#334155',
      800: '#1e293b',
      900: '#0f172a',
      950: '#020617',
    },

    // Background Colors (Dark Theme)
    background: {
      primary: '#0f172a',      // Main background
      secondary: '#1e293b',    // Cards, panels
      tertiary: '#334155',     // Hover states
      elevated: '#475569',     // Modals, tooltips
    },

    // Text Colors
    text: {
      primary: '#f8fafc',      // Main text
      secondary: '#cbd5e1',    // Secondary text
      tertiary: '#94a3b8',     // Muted text
      disabled: '#64748b',     // Disabled text
      inverse: '#0f172a',      // Text on light backgrounds
    },

    // Border Colors
    border: {
      light: '#334155',        // Subtle borders
      DEFAULT: '#475569',      // Standard borders
      dark: '#64748b',         // Emphasized borders
      focus: '#3b82f6',        // Focus state
    },

    // Status Indicator Colors
    status: {
      online: '#22c55e',       // Connected/Active
      offline: '#ef4444',      // Disconnected/Error
      idle: '#f59e0b',         // Idle/Warning
      busy: '#8b5cf6',         // Processing
    },
  },

  /**
   * Typography Scale
   * Consistent font sizes and line heights
   */
  typography: {
    fontFamily: {
      sans: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      mono: '"SF Mono", "Monaco", "Inconsolata", "Fira Code", "Droid Sans Mono", "Source Code Pro", monospace',
    },

    fontSize: {
      xs: '0.75rem',      // 12px
      sm: '0.875rem',     // 14px
      base: '1rem',       // 16px
      lg: '1.125rem',     // 18px
      xl: '1.25rem',      // 20px
      '2xl': '1.5rem',    // 24px
      '3xl': '1.875rem',  // 30px
      '4xl': '2.25rem',   // 36px
    },

    fontWeight: {
      normal: 400,
      medium: 500,
      semibold: 600,
      bold: 700,
    },

    lineHeight: {
      tight: 1.25,
      normal: 1.5,
      relaxed: 1.75,
    },
  },

  /**
   * Spacing Scale
   * Consistent spacing throughout the app
   */
  spacing: {
    0: '0',
    1: '0.25rem',   // 4px
    2: '0.5rem',    // 8px
    3: '0.75rem',   // 12px
    4: '1rem',      // 16px
    5: '1.25rem',   // 20px
    6: '1.5rem',    // 24px
    8: '2rem',      // 32px
    10: '2.5rem',   // 40px
    12: '3rem',     // 48px
    16: '4rem',     // 64px
    20: '5rem',     // 80px
  },

  /**
   * Border Radius
   * Consistent rounded corners
   */
  borderRadius: {
    none: '0',
    sm: '0.25rem',    // 4px
    DEFAULT: '0.375rem',  // 6px
    md: '0.5rem',     // 8px
    lg: '0.75rem',    // 12px
    xl: '1rem',       // 16px
    '2xl': '1.5rem',  // 24px
    full: '9999px',   // Circular
  },

  /**
   * Shadows
   * Depth and elevation
   */
  shadows: {
    none: 'none',
    sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
    DEFAULT: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
    md: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
    lg: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
    xl: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
    '2xl': '0 25px 50px -12px rgb(0 0 0 / 0.25)',
    inner: 'inset 0 2px 4px 0 rgb(0 0 0 / 0.05)',
    
    // Glow effects
    glow: {
      primary: '0 0 20px rgba(59, 130, 246, 0.3)',
      success: '0 0 20px rgba(34, 197, 94, 0.3)',
      error: '0 0 20px rgba(239, 68, 68, 0.3)',
    },
  },

  /**
   * Transitions
   * Smooth animations
   */
  transitions: {
    fast: '150ms cubic-bezier(0.4, 0, 0.2, 1)',
    DEFAULT: '200ms cubic-bezier(0.4, 0, 0.2, 1)',
    slow: '300ms cubic-bezier(0.4, 0, 0.2, 1)',
    
    // Easing functions
    easing: {
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
      easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
    },
  },

  /**
   * Z-Index Scale
   * Layering hierarchy
   */
  zIndex: {
    base: 0,
    dropdown: 1000,
    sticky: 1100,
    fixed: 1200,
    modalBackdrop: 1300,
    modal: 1400,
    popover: 1500,
    tooltip: 1600,
  },

  /**
   * Component Variants
   * Pre-defined component styles
   */
  components: {
    button: {
      // Primary Button
      primary: {
        background: '#3b82f6',
        backgroundHover: '#2563eb',
        color: '#ffffff',
        border: 'none',
        shadow: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
        shadowHover: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
      },
      
      // Secondary Button
      secondary: {
        background: '#334155',
        backgroundHover: '#475569',
        color: '#f8fafc',
        border: '1px solid #475569',
        shadow: 'none',
        shadowHover: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
      },
      
      // Ghost Button
      ghost: {
        background: 'transparent',
        backgroundHover: '#1e293b',
        color: '#cbd5e1',
        border: 'none',
        shadow: 'none',
        shadowHover: 'none',
      },
      
      // Danger Button
      danger: {
        background: '#ef4444',
        backgroundHover: '#dc2626',
        color: '#ffffff',
        border: 'none',
        shadow: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
        shadowHover: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
      },
    },

    input: {
      background: '#1e293b',
      backgroundFocus: '#334155',
      border: '#475569',
      borderFocus: '#3b82f6',
      color: '#f8fafc',
      placeholder: '#64748b',
      shadow: 'inset 0 2px 4px 0 rgb(0 0 0 / 0.05)',
      shadowFocus: '0 0 0 3px rgba(59, 130, 246, 0.1)',
    },

    card: {
      background: '#1e293b',
      backgroundHover: '#334155',
      border: '#334155',
      shadow: '0 1px 3px 0 rgb(0 0 0 / 0.1)',
      shadowHover: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    },

    badge: {
      success: {
        background: 'rgba(34, 197, 94, 0.1)',
        color: '#86efac',
        border: 'rgba(34, 197, 94, 0.2)',
      },
      warning: {
        background: 'rgba(245, 158, 11, 0.1)',
        color: '#fcd34d',
        border: 'rgba(245, 158, 11, 0.2)',
      },
      error: {
        background: 'rgba(239, 68, 68, 0.1)',
        color: '#fca5a5',
        border: 'rgba(239, 68, 68, 0.2)',
      },
      info: {
        background: 'rgba(14, 165, 233, 0.1)',
        color: '#7dd3fc',
        border: 'rgba(14, 165, 233, 0.2)',
      },
    },

    tooltip: {
      background: '#475569',
      color: '#f8fafc',
      shadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)',
    },

    modal: {
      backdrop: 'rgba(15, 23, 42, 0.8)',
      background: '#1e293b',
      border: '#334155',
      shadow: '0 25px 50px -12px rgb(0 0 0 / 0.25)',
    },
  },
};

/**
 * Helper function to get theme values
 */
export const getThemeValue = (path: string): any => {
  return path.split('.').reduce((obj, key) => obj?.[key], theme as any);
};

/**
 * CSS Variables Export
 * For use in global styles
 */
export const cssVariables = `
  :root {
    /* Colors */
    --color-primary: ${theme.colors.primary[500]};
    --color-success: ${theme.colors.success.DEFAULT};
    --color-warning: ${theme.colors.warning.DEFAULT};
    --color-error: ${theme.colors.error.DEFAULT};
    
    /* Backgrounds */
    --bg-primary: ${theme.colors.background.primary};
    --bg-secondary: ${theme.colors.background.secondary};
    --bg-tertiary: ${theme.colors.background.tertiary};
    
    /* Text */
    --text-primary: ${theme.colors.text.primary};
    --text-secondary: ${theme.colors.text.secondary};
    --text-tertiary: ${theme.colors.text.tertiary};
    
    /* Borders */
    --border-color: ${theme.colors.border.DEFAULT};
    --border-focus: ${theme.colors.border.focus};
    
    /* Spacing */
    --spacing-unit: 0.25rem;
    
    /* Border Radius */
    --radius-default: ${theme.borderRadius.DEFAULT};
    --radius-lg: ${theme.borderRadius.lg};
    
    /* Transitions */
    --transition-default: ${theme.transitions.DEFAULT};
  }
`;

export default theme;
