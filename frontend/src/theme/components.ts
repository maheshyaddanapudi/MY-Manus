/**
 * Reusable Component Style Utilities
 * Pre-built className strings for common components
 */

import { theme } from './theme';

/**
 * Button Styles
 */
export const buttonStyles = {
  base: 'inline-flex items-center justify-center font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-900 disabled:opacity-50 disabled:cursor-not-allowed',
  
  sizes: {
    sm: 'px-3 py-1.5 text-sm rounded-md',
    md: 'px-4 py-2 text-base rounded-lg',
    lg: 'px-6 py-3 text-lg rounded-lg',
  },
  
  variants: {
    primary: 'bg-blue-600 hover:bg-blue-700 text-white shadow-sm hover:shadow-md focus:ring-blue-500',
    secondary: 'bg-gray-700 hover:bg-gray-600 text-gray-100 border border-gray-600 focus:ring-gray-500',
    ghost: 'bg-transparent hover:bg-gray-800 text-gray-300 hover:text-white focus:ring-gray-700',
    danger: 'bg-red-600 hover:bg-red-700 text-white shadow-sm hover:shadow-md focus:ring-red-500',
    success: 'bg-green-600 hover:bg-green-700 text-white shadow-sm hover:shadow-md focus:ring-green-500',
  },
  
  icon: 'p-2 rounded-lg hover:bg-gray-800 text-gray-400 hover:text-gray-200 transition-colors',
};

/**
 * Input Styles
 */
export const inputStyles = {
  base: 'w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200',
  
  textarea: 'w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-3 text-gray-100 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 resize-none',
  
  select: 'w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 cursor-pointer',
  
  checkbox: 'w-4 h-4 bg-gray-800 border-gray-700 rounded text-blue-600 focus:ring-blue-500 focus:ring-offset-gray-900',
};

/**
 * Card Styles
 */
export const cardStyles = {
  base: 'bg-gray-800 border border-gray-700 rounded-lg shadow-sm transition-all duration-200',
  
  hover: 'hover:bg-gray-750 hover:shadow-md hover:border-gray-600',
  
  interactive: 'bg-gray-800 border border-gray-700 rounded-lg shadow-sm hover:bg-gray-750 hover:shadow-md hover:border-gray-600 cursor-pointer transition-all duration-200',
  
  elevated: 'bg-gray-800 border border-gray-700 rounded-lg shadow-lg',
};

/**
 * Badge Styles
 */
export const badgeStyles = {
  base: 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
  
  variants: {
    success: 'bg-green-900/30 text-green-400 border border-green-800',
    warning: 'bg-yellow-900/30 text-yellow-400 border border-yellow-800',
    error: 'bg-red-900/30 text-red-400 border border-red-800',
    info: 'bg-blue-900/30 text-blue-400 border border-blue-800',
    default: 'bg-gray-700 text-gray-300 border border-gray-600',
  },
  
  dot: 'w-2 h-2 rounded-full',
};

/**
 * Panel Styles
 */
export const panelStyles = {
  container: 'flex flex-col h-full bg-gray-900 border-l border-gray-700',
  
  header: 'flex items-center justify-between px-4 py-3 border-b border-gray-700 bg-gray-800/50',
  
  title: 'text-sm font-semibold text-gray-200',
  
  content: 'flex-1 overflow-auto p-4',
  
  footer: 'px-4 py-3 border-t border-gray-700 bg-gray-800/50',
};

/**
 * Message/Chat Styles
 */
export const messageStyles = {
  container: 'flex flex-col space-y-4 p-4',
  
  message: {
    base: 'flex flex-col space-y-1 max-w-3xl',
    user: 'items-end self-end',
    agent: 'items-start self-start',
  },
  
  bubble: {
    user: 'bg-blue-600 text-white rounded-2xl rounded-tr-sm px-4 py-2.5 shadow-sm',
    agent: 'bg-gray-800 text-gray-100 rounded-2xl rounded-tl-sm px-4 py-2.5 shadow-sm border border-gray-700',
  },
  
  timestamp: 'text-xs text-gray-500 px-2',
  
  avatar: 'w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-sm font-medium',
};

/**
 * List Styles
 */
export const listStyles = {
  container: 'space-y-1',
  
  item: {
    base: 'flex items-center px-3 py-2 rounded-lg cursor-pointer transition-all duration-200',
    default: 'text-gray-300 hover:bg-gray-800 hover:text-white',
    active: 'bg-gray-800 text-white font-medium',
    disabled: 'text-gray-600 cursor-not-allowed',
  },
  
  icon: 'w-5 h-5 mr-3 text-gray-400',
};

/**
 * Modal Styles
 */
export const modalStyles = {
  backdrop: 'fixed inset-0 bg-gray-900/80 backdrop-blur-sm z-50 flex items-center justify-center p-4',
  
  container: 'bg-gray-800 border border-gray-700 rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden',
  
  header: 'flex items-center justify-between px-6 py-4 border-b border-gray-700',
  
  title: 'text-lg font-semibold text-gray-100',
  
  close: 'p-2 rounded-lg hover:bg-gray-700 text-gray-400 hover:text-gray-200 transition-colors',
  
  content: 'p-6 overflow-auto',
  
  footer: 'flex items-center justify-end space-x-3 px-6 py-4 border-t border-gray-700 bg-gray-800/50',
};

/**
 * Tooltip Styles
 */
export const tooltipStyles = {
  container: 'absolute z-50 px-3 py-2 text-sm text-white bg-gray-700 rounded-lg shadow-lg',
  
  arrow: 'absolute w-2 h-2 bg-gray-700 transform rotate-45',
};

/**
 * Loading Styles
 */
export const loadingStyles = {
  spinner: 'inline-block w-5 h-5 border-2 border-gray-600 border-t-blue-500 rounded-full animate-spin',
  
  skeleton: 'animate-pulse bg-gray-700 rounded',
  
  dots: 'flex space-x-1',
  dot: 'w-2 h-2 bg-gray-500 rounded-full animate-bounce',
};

/**
 * Scrollbar Styles (for custom scrollbars)
 */
export const scrollbarStyles = `
  /* Custom Scrollbar */
  .custom-scrollbar::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  
  .custom-scrollbar::-webkit-scrollbar-track {
    background: ${theme.colors.background.primary};
  }
  
  .custom-scrollbar::-webkit-scrollbar-thumb {
    background: ${theme.colors.gray[700]};
    border-radius: 4px;
  }
  
  .custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: ${theme.colors.gray[600]};
  }
`;

/**
 * Animation Keyframes
 */
export const animations = {
  fadeIn: 'animate-[fadeIn_200ms_ease-in]',
  fadeOut: 'animate-[fadeOut_200ms_ease-out]',
  slideIn: 'animate-[slideIn_300ms_ease-out]',
  slideOut: 'animate-[slideOut_300ms_ease-in]',
  scaleIn: 'animate-[scaleIn_200ms_ease-out]',
  scaleOut: 'animate-[scaleOut_200ms_ease-in]',
  pulse: 'animate-pulse',
  spin: 'animate-spin',
  bounce: 'animate-bounce',
};

/**
 * Utility: Combine class names
 */
export const cn = (...classes: (string | undefined | null | false)[]): string => {
  return classes.filter(Boolean).join(' ');
};

/**
 * Utility: Get button classes
 */
export const getButtonClasses = (variant: keyof typeof buttonStyles.variants = 'primary', size: keyof typeof buttonStyles.sizes = 'md') => {
  return cn(buttonStyles.base, buttonStyles.sizes[size], buttonStyles.variants[variant]);
};

/**
 * Utility: Get badge classes
 */
export const getBadgeClasses = (variant: keyof typeof badgeStyles.variants = 'default') => {
  return cn(badgeStyles.base, badgeStyles.variants[variant]);
};

export default {
  button: buttonStyles,
  input: inputStyles,
  card: cardStyles,
  badge: badgeStyles,
  panel: panelStyles,
  message: messageStyles,
  list: listStyles,
  modal: modalStyles,
  tooltip: tooltipStyles,
  loading: loadingStyles,
  animations,
  cn,
  getButtonClasses,
  getBadgeClasses,
};
