import { useEffect, useState } from "react";
import api from "../services/api";
import type { Flight } from "../types/Flight";

function Flights() {
  const [flights, setFlights] = useState<Flight[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const loadFlights = async () => {
    try {
      setLoading(true);

      const response = await api.get<Flight[]>("/flights");

      setFlights(response.data);
      setError("");
    } catch (err) {
      console.error(err);
      setError("Unable to load flights.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFlights();
  }, []);

  const updateFlightStatus = async (
    flightId: number,
    status: "CANCELLED" | "DELAYED"
  ) => {
    try {
      setUpdatingId(flightId);
      setError("");

      await api.patch(`/flights/${flightId}/status`, {
        status,
      });

      await loadFlights();
    } catch (err) {
      console.error(err);
      setError("Unable to update flight status.");
    } finally {
      setUpdatingId(null);
    }
  };

  const getStatusClass = (status: string) => {
    return `status status-${status.toLowerCase()}`;
  };

  if (loading && flights.length === 0) {
    return <p>Loading flights...</p>;
  }

  return (
    <div>
      <div className="page-title">
        <div>
          <h2>Flights</h2>
          <p>Manage flight operations and disruptions.</p>
        </div>

        <button className="refresh-button" onClick={loadFlights}>
          Refresh
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}

      <table>
        <thead>
          <tr>
            <th>Flight</th>
            <th>Route</th>
            <th>Departure</th>
            <th>Status</th>
            <th>Seats</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          {flights.map((flight) => (
            <tr key={flight.id}>
              <td>
                <strong>{flight.flightNumber}</strong>
              </td>

              <td>
                {flight.origin} → {flight.destination}
              </td>

              <td>
                {new Date(flight.departureTime).toLocaleString()}
              </td>

              <td>
                <span className={getStatusClass(flight.status)}>
                  {flight.status}
                </span>
              </td>

              <td>{flight.availableSeats}</td>

              <td>
                {flight.status !== "CANCELLED" &&
                  flight.status !== "DEPARTED" &&
                  flight.status !== "ARRIVED" && (
                    <div className="actions">
                      <button
                        className="delay-button"
                        disabled={updatingId === flight.id}
                        onClick={() =>
                          updateFlightStatus(flight.id, "DELAYED")
                        }
                      >
                        Delay
                      </button>

                      <button
                        className="cancel-button"
                        disabled={updatingId === flight.id}
                        onClick={() =>
                          updateFlightStatus(flight.id, "CANCELLED")
                        }
                      >
                        Cancel
                      </button>
                    </div>
                  )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Flights;