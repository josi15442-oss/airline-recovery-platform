import type { Flight } from "./Flight";

export interface Booking {
  id: number;
  passengerId: string;
  flight: Flight;
  status:
    | "CONFIRMED"
    | "REBOOKING_REQUIRED"
    | "REBOOKED"
    | "CANCELLED";
  createdAt: string;
}