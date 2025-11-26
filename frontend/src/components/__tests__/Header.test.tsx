import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Header } from '../Layout/Header';

describe('Header', () => {
  it('renders header', () => {
    render(<Header />);
    expect(screen.getByText(/MY Manus/)).toBeInTheDocument();
  });

  it('displays logo', () => {
    render(<Header />);
    const logo = screen.getByAltText(/MY Manus/);
    expect(logo).toBeInTheDocument();
  });

  it('displays navigation items', () => {
    render(<Header />);
    expect(screen.getByText(/Events/)).toBeInTheDocument();
    expect(screen.getByText(/Browser/)).toBeInTheDocument();
    expect(screen.getByText(/Chat/)).toBeInTheDocument();
  });

  it('applies correct styling', () => {
    const { container } = render(<Header />);
    expect(container.querySelector('.header')).toBeInTheDocument();
  });
});
