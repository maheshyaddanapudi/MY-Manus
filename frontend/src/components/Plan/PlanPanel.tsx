import React, { useState, useEffect } from 'react';
import { useWebSocket } from '../../hooks/useWebSocket';

interface TodoStructure {
  title: string;
  tasks: TodoTask[];
  sections: Record<string, string>;
  lastUpdated: string;
}

interface TodoTask {
  taskNumber: number;
  description: string;
  completed: boolean;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  notes?: string;
}

interface PlanPanelProps {
  sessionId: string;
}

export const PlanPanel: React.FC<PlanPanelProps> = ({ sessionId }) => {
  const [plan, setPlan] = useState<TodoStructure | null>(null);
  const [loading, setLoading] = useState(true);

  // WebSocket for live updates
  useWebSocket(`/topic/plan/${sessionId}`, (updatedPlan: TodoStructure) => {
    setPlan(updatedPlan);
  });

  useEffect(() => {
    loadPlan();
    startWatching();
  }, [sessionId]);

  const loadPlan = async () => {
    try {
      const response = await fetch(`/api/plan/${sessionId}`);
      if (response.ok) {
        const data = await response.json();
        setPlan(data);
      }
    } catch (error) {
      console.error('Failed to load plan:', error);
    } finally {
      setLoading(false);
    }
  };

  const startWatching = async () => {
    try {
      await fetch(`/api/plan/${sessionId}/watch`, { method: 'POST' });
    } catch (error) {
      console.error('Failed to start watching:', error);
    }
  };

  const getStatusIcon = (status: TodoTask['status']) => {
    switch (status) {
      case 'COMPLETED':
        return '✅';
      case 'IN_PROGRESS':
        return '🔄';
      default:
        return '⏳';
    }
  };

  const getStatusColor = (status: TodoTask['status']) => {
    switch (status) {
      case 'COMPLETED':
        return 'border-green-500 bg-green-900/20';
      case 'IN_PROGRESS':
        return 'border-blue-500 bg-blue-900/20';
      default:
        return 'border-gray-700 bg-gray-800';
    }
  };

  if (loading) {
    return <div className="p-4 text-gray-400">Loading plan...</div>;
  }

  if (!plan) {
    return (
      <div className="h-full flex items-center justify-center text-gray-400">
        <div className="text-center">
          <div className="text-4xl mb-2">📋</div>
          <div>No plan yet</div>
          <div className="text-sm mt-1">
            Agent will create todo.md when planning
          </div>
        </div>
      </div>
    );
  }

  const completedCount = plan.tasks.filter(t => t.completed).length;
  const progress = plan.tasks.length > 0 ? (completedCount / plan.tasks.length) * 100 : 0;

  return (
    <div className="h-full flex flex-col bg-gray-900 text-gray-100">
      {/* Header */}
      <div className="border-b border-gray-700 px-4 py-3">
        <h2 className="text-lg font-semibold">{plan.title}</h2>
        <div className="text-xs text-gray-400 mt-1">
          {completedCount} of {plan.tasks.length} tasks completed
        </div>

        {/* Progress Bar */}
        <div className="mt-3 bg-gray-700 rounded-full h-2">
          <div
            className="bg-blue-500 h-2 rounded-full transition-all duration-300"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {/* Tasks */}
      <div className="flex-1 overflow-y-auto p-4">
        {plan.tasks.length === 0 ? (
          <div className="text-center text-gray-400 py-8">
            No tasks in plan yet
          </div>
        ) : (
          <div className="space-y-2">
            {plan.tasks.map((task) => (
              <div
                key={task.taskNumber}
                className={`p-3 rounded border ${getStatusColor(task.status)}`}
              >
                <div className="flex items-start space-x-3">
                  <span className="text-xl flex-shrink-0">
                    {getStatusIcon(task.status)}
                  </span>
                  <div className="flex-1 min-w-0">
                    <div className={`font-medium ${
                      task.completed ? 'line-through text-gray-500' : 'text-white'
                    }`}>
                      <span className="text-gray-400 text-sm mr-2">#{task.taskNumber}</span>
                      {task.description}
                    </div>
                    {task.notes && (
                      <div className="text-sm text-gray-400 mt-1">
                        {task.notes}
                      </div>
                    )}
                    {task.status === 'IN_PROGRESS' && (
                      <div className="mt-2 flex items-center space-x-2 text-sm text-blue-400">
                        <div className="animate-spin">⏳</div>
                        <span>In progress...</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Additional Sections */}
      {Object.entries(plan.sections || {}).map(([sectionName, content]) => (
        sectionName !== 'tasks' && content && (
          <div key={sectionName} className="border-t border-gray-700 px-4 py-3 max-h-32 overflow-y-auto">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">
              {sectionName}
            </h3>
            <div className="text-sm text-gray-300 whitespace-pre-wrap">
              {content}
            </div>
          </div>
        )
      ))}

      {/* Footer */}
      <div className="border-t border-gray-700 px-4 py-2 bg-gray-800">
        <div className="text-xs text-gray-400">
          Last updated: {new Date(plan.lastUpdated).toLocaleTimeString()}
        </div>
      </div>
    </div>
  );
};
