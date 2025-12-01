import React, { useState, useEffect } from 'react';

interface ReplayPoint {
  iteration: number;
  eventCount: number;
  firstEventId: string;  // UUID
  lastEventId: string;   // UUID
  summary: string;
}

interface StateSnapshot {
  sessionId: string;
  eventCount: number;
  currentEventId: string;  // UUID
  currentSequence: number;
  iteration: number;
  pythonVariables: Record<string, any>;
  events: any[];
  lastAction?: string;
  lastObservation?: any;
}

interface SessionReplayPanelProps {
  sessionId: string;
}

export const SessionReplayPanel: React.FC<SessionReplayPanelProps> = ({ sessionId }) => {
  const [replayPoints, setReplayPoints] = useState<ReplayPoint[]>([]);
  const [currentSnapshot, setCurrentSnapshot] = useState<StateSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState(1000); // ms per step

  useEffect(() => {
    loadReplayPoints();
  }, [sessionId]);

  const loadReplayPoints = async () => {
    setLoading(true);
    try {
      const response = await fetch(`/api/replay/${sessionId}/points`);
      const points = await response.json();
      setReplayPoints(points);

      // Load first point
      if (points.length > 0) {
        await loadIteration(points[0].iteration);
      }
    } catch (error) {
      console.error('Failed to load replay points:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadIteration = async (iteration: number) => {
    setLoading(true);
    try {
      const response = await fetch(`/api/replay/${sessionId}/iteration/${iteration}`);
      const snapshot = await response.json();
      setCurrentSnapshot(snapshot);
    } catch (error) {
      console.error('Failed to load iteration:', error);
    } finally {
      setLoading(false);
    }
  };

  const stepForward = async () => {
    if (!currentSnapshot) return;

    setLoading(true);
    try {
      const response = await fetch(
        `/api/replay/${sessionId}/step-forward/${currentSnapshot.currentEventId}`
      );
      const snapshot = await response.json();

      if (snapshot.success === false) {
        setIsPlaying(false);
        return;
      }

      setCurrentSnapshot(snapshot);
    } catch (error) {
      console.error('Failed to step forward:', error);
      setIsPlaying(false);
    } finally {
      setLoading(false);
    }
  };

  const stepBackward = async () => {
    if (!currentSnapshot) return;

    setLoading(true);
    try {
      const response = await fetch(
        `/api/replay/${sessionId}/step-backward/${currentSnapshot.currentEventId}`
      );
      const snapshot = await response.json();

      if (snapshot.success === false) {
        return;
      }

      setCurrentSnapshot(snapshot);
    } catch (error) {
      console.error('Failed to step backward:', error);
    } finally {
      setLoading(false);
    }
  };

  const togglePlayback = () => {
    setIsPlaying(!isPlaying);
  };

  // Auto-play effect
  useEffect(() => {
    if (!isPlaying) return;

    const interval = setInterval(() => {
      stepForward();
    }, playbackSpeed);

    return () => clearInterval(interval);
  }, [isPlaying, playbackSpeed, currentSnapshot]);

  return (
    <div className="h-full flex flex-col bg-gray-900 text-gray-100">
      {/* Header */}
      <div className="border-b border-gray-700 px-4 py-3">
        <h2 className="text-lg font-semibold">Session Replay</h2>
        <div className="text-xs text-gray-400 mt-1">
          Time-travel debugging - Step through execution history
        </div>
      </div>

      {/* Controls */}
      <div className="border-b border-gray-700 px-4 py-3 flex items-center space-x-4">
        <button
          onClick={stepBackward}
          disabled={loading || !currentSnapshot}
          className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm disabled:opacity-50"
        >
          ⏮️ Step Back
        </button>

        <button
          onClick={togglePlayback}
          disabled={loading || !currentSnapshot}
          className="px-3 py-1 bg-blue-600 hover:bg-blue-500 rounded text-sm disabled:opacity-50"
        >
          {isPlaying ? '⏸️ Pause' : '▶️ Play'}
        </button>

        <button
          onClick={stepForward}
          disabled={loading || !currentSnapshot}
          className="px-3 py-1 bg-gray-700 hover:bg-gray-600 rounded text-sm disabled:opacity-50"
        >
          ⏭️ Step Forward
        </button>

        <div className="flex items-center space-x-2">
          <label className="text-sm text-gray-400">Speed:</label>
          <select
            value={playbackSpeed}
            onChange={(e) => setPlaybackSpeed(Number(e.target.value))}
            className="bg-gray-700 border border-gray-600 rounded px-2 py-1 text-sm"
          >
            <option value={2000}>0.5x</option>
            <option value={1000}>1x</option>
            <option value={500}>2x</option>
            <option value={250}>4x</option>
          </select>
        </div>

        {currentSnapshot && (
          <div className="ml-auto text-sm text-gray-400">
            Iteration: {currentSnapshot.iteration} | Event: {currentSnapshot.currentEventId} ({currentSnapshot.currentSequence})
          </div>
        )}
      </div>

      {/* Content */}
      <div className="flex-1 flex overflow-hidden">
        {/* Timeline */}
        <div className="w-1/4 border-r border-gray-700 overflow-y-auto">
          <div className="p-4">
            <h3 className="text-sm font-semibold mb-3">Timeline</h3>
            {replayPoints.map((point) => (
              <div
                key={point.iteration}
                onClick={() => loadIteration(point.iteration)}
                className={`p-3 mb-2 rounded cursor-pointer border transition-colors ${
                  currentSnapshot?.iteration === point.iteration
                    ? 'bg-blue-900 border-blue-500'
                    : 'bg-gray-800 border-gray-700 hover:bg-gray-750'
                }`}
              >
                <div className="flex items-center justify-between mb-1">
                  <span className="font-semibold">Iteration {point.iteration}</span>
                  <span className="text-xs text-gray-400">{point.eventCount} events</span>
                </div>
                <div className="text-xs text-gray-400">{point.summary}</div>
              </div>
            ))}
          </div>
        </div>

        {/* State Viewer */}
        <div className="flex-1 overflow-y-auto p-4">
          {loading && <div className="text-gray-400">Loading...</div>}

          {!loading && !currentSnapshot && (
            <div className="text-gray-400">No snapshot loaded</div>
          )}

          {!loading && currentSnapshot && (
            <div className="space-y-4">
              {/* Last Action */}
              {currentSnapshot.lastAction && (
                <div>
                  <h3 className="text-sm font-semibold mb-2">Last Action</h3>
                  <pre className="bg-gray-800 p-3 rounded text-xs overflow-x-auto">
                    <code>{currentSnapshot.lastAction}</code>
                  </pre>
                </div>
              )}

              {/* Last Observation */}
              {currentSnapshot.lastObservation && (
                <div>
                  <h3 className="text-sm font-semibold mb-2">Last Observation</h3>
                  <div className="bg-gray-800 p-3 rounded text-xs">
                    {currentSnapshot.lastObservation.stdout && (
                      <div>
                        <div className="text-green-400 font-semibold mb-1">stdout:</div>
                        <pre className="text-gray-300">{currentSnapshot.lastObservation.stdout}</pre>
                      </div>
                    )}
                    {currentSnapshot.lastObservation.stderr && (
                      <div className="mt-2">
                        <div className="text-red-400 font-semibold mb-1">stderr:</div>
                        <pre className="text-gray-300">{currentSnapshot.lastObservation.stderr}</pre>
                      </div>
                    )}
                    {currentSnapshot.lastObservation.exitCode !== undefined && (
                      <div className="mt-2 text-gray-400">
                        Exit code: {currentSnapshot.lastObservation.exitCode}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Python Variables */}
              {Object.keys(currentSnapshot.pythonVariables).length > 0 && (
                <div>
                  <h3 className="text-sm font-semibold mb-2">Python Variables</h3>
                  <div className="bg-gray-800 p-3 rounded text-xs space-y-1">
                    {Object.entries(currentSnapshot.pythonVariables).map(([key, value]) => (
                      <div key={key} className="flex">
                        <span className="text-blue-400 font-mono mr-2">{key}:</span>
                        <span className="text-gray-300 font-mono">
                          {JSON.stringify(value, null, 2)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Events in this snapshot */}
              <div>
                <h3 className="text-sm font-semibold mb-2">
                  Events ({currentSnapshot.eventCount})
                </h3>
                <div className="space-y-2">
                  {currentSnapshot.events.slice(-10).map((event: any) => (
                    <div
                      key={event.id}
                      className="bg-gray-800 p-2 rounded text-xs"
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-semibold">{event.type}</span>
                        <span className="text-gray-400">Seq: {event.sequence}</span>
                      </div>
                      {event.data && (
                        <pre className="text-gray-400 text-xs overflow-x-auto">
                          {JSON.stringify(event.data, null, 2).substring(0, 200)}
                        </pre>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
