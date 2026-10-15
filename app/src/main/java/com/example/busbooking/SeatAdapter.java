package com.example.busbooking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import java.util.List;
import java.util.Set;

public class SeatAdapter extends BaseAdapter {

    private final Context context;
    private final List<Seat> seatList;
    private final Set<Integer> selectedSeatNumbers; // currently selected but not yet booked

    public SeatAdapter(Context context, List<Seat> seatList, Set<Integer> selectedSeatNumbers) {
        this.context = context;
        this.seatList = seatList;
        this.selectedSeatNumbers = selectedSeatNumbers;
    }

    @Override
    public int getCount() {
        return seatList.size();
    }

    @Override
    public Object getItem(int position) {
        return seatList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Button seatButton;
        if (convertView == null) {
            seatButton = (Button) LayoutInflater.from(context)
                    .inflate(R.layout.seat_item, parent, false);
        } else {
            seatButton = (Button) convertView;
        }

        Seat seat = seatList.get(position);
        seatButton.setText(String.valueOf(seat.getSeatNumber()));

        if (seat.isBooked()) {
            seatButton.setBackgroundResource(R.drawable.seat_booked_bg);
            seatButton.setEnabled(false);
        } else if (selectedSeatNumbers.contains(seat.getSeatNumber())) {
            seatButton.setBackgroundResource(R.drawable.seat_selected_bg);
            seatButton.setEnabled(true);
        } else {
            seatButton.setBackgroundResource(R.drawable.seat_available_bg);
            seatButton.setEnabled(true);
        }

        return seatButton;
    }
}