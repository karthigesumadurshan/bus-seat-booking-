package com.example.busbooking;

public class Seat {

    private final int seatNumber;   // 1 to 54
    private boolean booked;
    private String passengerName;   // null if not booked

    public Seat(int seatNumber) {
        this.seatNumber = seatNumber;
        this.booked = false;
        this.passengerName = null;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public boolean isBooked() {
        return booked;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }
}