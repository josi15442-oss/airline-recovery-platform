import { useEffect, useState } from "react";
import api from "../services/api";
import type { Flight } from "../types/Flight";
import type { Booking } from "../types/Booking";

function Dashboard() {
  const [flights, setFlights] = useState<Flight[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      setLoading(true);

      const [flightResponse, bookingResponse] = await Promise.all([
        api.get<Flight[]>("/flights"),
        api.get<Booking[]>("/bookings"),
      ]);

      setFlights(flightResponse.data);
      setBookings(bookingResponse.data);
      setError("");
    } catch (err) {
      console.error(err);
      setError("Unable to load dashboard.");
    } finally {
      setLoading(false);
    }
  };

  const totalFlights = flights.length;

  const scheduledFlights = flights.filter(
    (flight) =>
      flight.status === "SCHEDULED" ||
      flight.status === "ON_TIME"
  ).length;

  const delayedFlights = flights.filter(
    (flight) => flight.status === "DELAYED"
  ).length;

  const cancelledFlights = flights.filter(
    (flight) => flight.status === "CANCELLED"
  ).length;

  const availableSeats = flights.reduce(
    (total, flight) => total + flight.availableSeats,
    0
  );

  const rebookedPassengers = bookings.filter(
    (booking) => booking.status === "REBOOKED"
  ).length;

  const recoveryRequired = bookings.filter(
    (booking) => booking.status === "REBOOKING_REQUIRED"
  ).length;

  const confirmedPassengers = bookings.filter(
    (booking) => booking.status === "CONFIRMED"
  ).length;

  if (loading) {
    return <p>Loading dashboard...</p>;
  }

  return (
    <div>
      <div className="page-title">
        <div>
          <h2>Operations Dashboard</h2>
          <p>
            Monitor flight operations and passenger recovery.
          </p>
        </div>

        <button
          className="refresh-button"
          onClick={loadDashboard}
        >
          Refresh
        </button>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      <div className="stats-grid">
        <div className="stat-card">
          <span>Total Flights</span>
          <strong>{totalFlights}</strong>
        </div>

        <div className="stat-card">
          <span>Scheduled</span>
          <strong>{scheduledFlights}</strong>
        </div>

        <div className="stat-card">
          <span>Delayed</span>
          <strong>{delayedFlights}</strong>
        </div>

        <div className="stat-card">
          <span>Cancelled</span>
          <strong>{cancelledFlights}</strong>
        </div>

        <div className="stat-card">
          <span>Available Seats</span>
          <strong>{availableSeats}</strong>
        </div>

        <div className="stat-card">
          <span>Confirmed Passengers</span>
          <strong>{confirmedPassengers}</strong>
        </div>

        <div className="stat-card">
          <span>Rebooked Passengers</span>
          <strong>{rebookedPassengers}</strong>
        </div>

        <div className="stat-card">
          <span>Recovery Required</span>
          <strong>{recoveryRequired}</strong>
        </div>
      </div>

      <div className="dashboard-section">
        <h3>Recent Flight Operations</h3>

        <table>
          <thead>
            <tr>
              <th>Flight</th>
              <th>Route</th>
              <th>Departure</th>
              <th>Status</th>
              <th>Seats</th>
            </tr>
          </thead>

          <tbody>
            {flights
              .slice()
              .reverse()
              .slice(0, 6)
              .map((flight) => (
                <tr key={flight.id}>
                  <td>
                    <strong>{flight.flightNumber}</strong>
                  </td>

                  <td>
                    {flight.origin} → {flight.destination}
                  </td>

                  <td>
                    {new Date(
                      flight.departureTime
                    ).toLocaleString()}
                  </td>

                  <td>
                    <span
                      className={`status status-${flight.status.toLowerCase()}`}
                    >
                      {flight.status}
                    </span>
                  </td>

                  <td>{flight.availableSeats}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      <div className="dashboard-section">
        <h3>Recent Passenger Recovery</h3>

        {bookings.length === 0 ? (
          <p>No passenger bookings available.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Passenger</th>
                <th>Flight</th>
                <th>Route</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {bookings
                .slice()
                .reverse()
                .slice(0, 6)
                .map((booking) => (
                  <tr key={booking.id}>
                    <td>
                      <strong>{booking.passengerId}</strong>
                    </td>

                    <td>{booking.flight.flightNumber}</td>

                    <td>
                      {booking.flight.origin} →{" "}
                      {booking.flight.destination}
                    </td>

                    <td>
                      <span
                        className={`status booking-status-${booking.status.toLowerCase()}`}
                      >
                        {booking.status}
                      </span>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

export default Dashboard;