package com.example.datepicker;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class DatePicker extends BasePicker {

    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;
    private static final boolean DEFAULT_ENABLED_STATE = true;
    private final LinearLayout mNPickers;

    private final NumberPicker mDayNPicker;
    private final NumberPicker mMonthNPicker;
    private final NumberPicker mYearNPicker;
    private Locale mCurrentLocale;
    private OnChangedListener mOnChangedListener;
    private String[] mShortMonths;
    private int mNumberOfMonths;

    private Calendar mTempDate;

    private Calendar mMinDate;

    private Calendar mMaxDate;

    private Calendar mCurrentDate;

    private boolean mIsEnabled = DEFAULT_ENABLED_STATE;

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCurrentLocale(Locale.getDefault());

        int startYear = DEFAULT_START_YEAR;
        int endYear = DEFAULT_END_YEAR;
        String minDate = "01/01/2021";
        String maxDate = "01/01/2121";

        LayoutInflater.from(getContext()).inflate(R.layout.picker, this);

        NumberPicker.OnValueChangeListener onChangeListener = new NumberPicker.OnValueChangeListener() {
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateInputState();
                mTempDate.setTimeInMillis(mCurrentDate.getTimeInMillis());
                // take care of wrapping of days and months to update greater
                // fields
                if (picker == mDayNPicker) {
                    int maxDayOfMonth = mTempDate
                            .getActualMaximum(Calendar.DAY_OF_MONTH);
                    if (oldVal == maxDayOfMonth && newVal == 1) {
                        mTempDate.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (oldVal == 1 && newVal == maxDayOfMonth) {
                        mTempDate.add(Calendar.DAY_OF_MONTH, -1);
                    } else {
                        mTempDate.add(Calendar.DAY_OF_MONTH, newVal - oldVal);
                    }
                } else if (picker == mMonthNPicker) {
                    if (oldVal == 11 && newVal == 0) {
                        mTempDate.add(Calendar.MONTH, 1);
                    } else if (oldVal == 0 && newVal == 11) {
                        mTempDate.add(Calendar.MONTH, -1);
                    } else {
                        mTempDate.add(Calendar.MONTH, newVal - oldVal);
                    }
                } else if (picker == mYearNPicker) {
                    mTempDate.set(Calendar.YEAR, newVal);
                } else {
                    throw new IllegalArgumentException();
                }
                // now set the date to the adjusted one
                setDate(mTempDate.get(Calendar.YEAR),
                        mTempDate.get(Calendar.MONTH),
                        mTempDate.get(Calendar.DAY_OF_MONTH));
                updateNPickers();
                notifyDateChanged();
            }
        };

        mNPickers = findViewById(R.id.pickers);
        // day
        mDayNPicker = findViewById(R.id.day_picker);
        mDayNPicker.setFormatter(NumberPicker.getTwoDigitFormatter());
        mDayNPicker.setOnLongPressUpdateInterval(100);
        mDayNPicker.setOnChangedListener(onChangeListener);

        // month
        mMonthNPicker = findViewById(R.id.month_picker);
        mMonthNPicker.setMinValue(0);
        mMonthNPicker.setMaxValue(mNumberOfMonths - 1);
        mMonthNPicker.setDisplayedValues(mShortMonths);
        mMonthNPicker.setOnLongPressUpdateInterval(200);
        mMonthNPicker.setOnChangedListener(onChangeListener);

        // year
        mYearNPicker = findViewById(R.id.year_picker);
        mYearNPicker.setOnLongPressUpdateInterval(100);
        mYearNPicker.setOnChangedListener(onChangeListener);

        // set the min date giving priority of the minDate over startYear
        mTempDate.clear();
        if (!TextUtils.isEmpty(minDate)) {
            if (!parseDate(minDate, mTempDate)) {
                mTempDate.set(startYear, 0, 1);
            }
        } else {
            mTempDate.set(startYear, 0, 1);
        }
        setMinDate(mTempDate.getTimeInMillis());

        // set the max date giving priority of the maxDate over endYear
        mTempDate.clear();
        if (!TextUtils.isEmpty(maxDate)) {
            if (!parseDate(maxDate, mTempDate)) {
                mTempDate.set(endYear, 11, 31);
            }
        } else {
            mTempDate.set(endYear, 11, 31);
        }
        setMaxDate(mTempDate.getTimeInMillis());

        // initialize to current date
        mCurrentDate.setTimeInMillis(System.currentTimeMillis());
        init(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH), mCurrentDate.get(Calendar.DAY_OF_MONTH));

        // re-order the number NPickers to match the current date format
        reorderNPickers();

        // If not explicitly specified this view is important for accessibility.
        if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    /**
     * Sets the minimal date supported by this {@link NumberPicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param minDate The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMinDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        mMinDate.setTimeInMillis(minDate);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        }
        updateNPickers();
    }


    /**
     * Sets the maximal date supported by this {@link com.example.datepicker.DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param maxDate The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMaxDate
                .get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        mMaxDate.setTimeInMillis(maxDate);
        if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
        updateNPickers();
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mDayNPicker.setEnabled(enabled);
        mMonthNPicker.setEnabled(enabled);
        mYearNPicker.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
        String selectedDateUtterance = DateUtils.formatDateTime(getContext(), mCurrentDate.getTimeInMillis(), flags);
        event.getText().add(selectedDateUtterance);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(com.example.datepicker.DatePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(com.example.datepicker.DatePicker.class.getName());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCurrentLocale(newConfig.locale);
    }

    /**
     * Sets whether the {@link CalendarView} is shown.
     *
     * @param shown True if the calendar view is to be shown.
     */
    public void setDayViewShown(boolean shown) {
        mDayNPicker.setVisibility(shown ? VISIBLE : GONE);
    }

    /**
     * Sets the current locale.
     *
     * @param locale The current locale.
     */
    private void setCurrentLocale(Locale locale) {
        if (locale.equals(mCurrentLocale)) {
            return;
        }

        mCurrentLocale = locale;

        mTempDate = getCalendarForLocale(mTempDate, locale);
        mMinDate = getCalendarForLocale(mMinDate, locale);
        mMaxDate = getCalendarForLocale(mMaxDate, locale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, locale);

        mNumberOfMonths = mTempDate.getActualMaximum(Calendar.MONTH) + 1;
        mShortMonths = new String[mNumberOfMonths];
        for (int i = 0; i < mNumberOfMonths; i++) {
            mShortMonths[i] = DateUtils.getMonthString(Calendar.JANUARY + i, DateUtils.LENGTH_MEDIUM);
        }
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given
     * calendar.
     *
     * @param oldCalendar The old calendar.
     * @param locale      The locale.
     */
    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    /**
     * Reorders the NPickers according to the date format that is explicitly set
     * by the user and if no such is set fall back to the current locale's
     * default format.
     */
    private void reorderNPickers() {
        mNPickers.removeAllViews();
        char[] order;
        try {
            order = DateFormat.getDateFormatOrder(getContext());
        } catch (IllegalArgumentException expected) {
            order = new char[0];
        }
        final int NPickerCount = order.length;
        for (int i = 0; i < NPickerCount; i++) {
            switch (order[i]) {
                case 'd':
                    mNPickers.addView(mDayNPicker);
                    setImeOptions(mDayNPicker, NPickerCount, i);
                    break;
                case 'M':
                    mNPickers.addView(mMonthNPicker);
                    setImeOptions(mMonthNPicker, NPickerCount, i);
                    break;
                case 'y':
                    mNPickers.addView(mYearNPicker);
                    setImeOptions(mYearNPicker, NPickerCount, i);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Updates the current date.
     *
     * @param year       The year.
     * @param month      The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth The day of the month.
     */
    public void updateDate(int year, int month, int dayOfMonth) {
        if (!isNewDate(year, month, dayOfMonth)) {
            return;
        }
        setDate(year, month, dayOfMonth);
        updateNPickers();
        notifyDateChanged();
    }

    // Override so we are in complete control of save / restore for this widget.
    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getYear(), getMonth(), getDayOfMonth());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setDate(ss.mYear, ss.mMonth, ss.mDay);
        updateNPickers();
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the NPickers.
     *
     * @param year        The initial year.
     * @param monthOfYear The initial month <strong>starting from zero</strong>.
     * @param dayOfMonth  The initial day of the month.
     */
    public void init(int year, int monthOfYear, int dayOfMonth) {
        setDate(year, monthOfYear, dayOfMonth);
        updateNPickers();
    }


    /**
     * Parses the given <code>date</code> and in case of success sets the result
     * to the <code>outDate</code>.
     *
     * @return True if the date was parsed.
     */
    private boolean parseDate(String date, Calendar outDate) {
        try {
            SimpleDateFormat mDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            outDate.setTime(Objects.requireNonNull(mDateFormat.parse(date)));
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isNewDate(int year, int month, int dayOfMonth) {
        return (mCurrentDate.get(Calendar.YEAR) != year
                || mCurrentDate.get(Calendar.MONTH) != dayOfMonth
                || mCurrentDate.get(Calendar.DAY_OF_MONTH) != month);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(year, month, dayOfMonth);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        } else if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
    }

    private void updateNPickers() {
        // set the NPicker ranges respecting the min and max dates
        if (mCurrentDate.equals(mMinDate)) {
            mDayNPicker.setMinValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            mDayNPicker.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            mDayNPicker.setWrapSelectorWheel(false);
            mMonthNPicker.setDisplayedValues(null);
            mMonthNPicker.setMinValue(mCurrentDate.get(Calendar.MONTH));
            mMonthNPicker.setMaxValue(mCurrentDate.getActualMaximum(Calendar.MONTH));
            mMonthNPicker.setWrapSelectorWheel(false);
        } else if (mCurrentDate.equals(mMaxDate)) {
            mDayNPicker.setMinValue(mCurrentDate.getActualMinimum(Calendar.DAY_OF_MONTH));
            mDayNPicker.setMaxValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            mDayNPicker.setWrapSelectorWheel(false);
            mMonthNPicker.setDisplayedValues(null);
            mMonthNPicker.setMinValue(mCurrentDate.getActualMinimum(Calendar.MONTH));
            mMonthNPicker.setMaxValue(mCurrentDate.get(Calendar.MONTH));
            mMonthNPicker.setWrapSelectorWheel(false);
        } else {
            mDayNPicker.setMinValue(1);
            mDayNPicker.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            mDayNPicker.setWrapSelectorWheel(true);
            mMonthNPicker.setDisplayedValues(null);
            mMonthNPicker.setMinValue(0);
            mMonthNPicker.setMaxValue(11);
            mMonthNPicker.setWrapSelectorWheel(true);
        }

        // make sure the month names are a zero based array
        // with the months in the month NPicker
        String[] displayedValues = Arrays.copyOfRange(mShortMonths, mMonthNPicker.getMinValue(), mMonthNPicker.getMaxValue() + 1);
        mMonthNPicker.setDisplayedValues(displayedValues);

        // year NPicker range does not change based on the current date
        mYearNPicker.setMinValue(mMinDate.get(Calendar.YEAR));
        mYearNPicker.setMaxValue(mMaxDate.get(Calendar.YEAR));
        mYearNPicker.setWrapSelectorWheel(false);

        // set the NPicker values
        mYearNPicker.setValue(mCurrentDate.get(Calendar.YEAR));
        mMonthNPicker.setValue(mCurrentDate.get(Calendar.MONTH));
        mDayNPicker.setValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * @return The selected year.
     */
    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    /**
     * @return The selected month.
     */
    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    /**
     * @return The selected day of month.
     */
    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Notifies the listener, if such, for a change in the selected date.
     */
    private void notifyDateChanged() {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnChangedListener != null) {
            mOnChangedListener.onChanged(this, getYear(), getMonth(), getDayOfMonth());
        }
    }

    /**
     * Sets the IME options for a NPicker based on its ordering.
     *
     * @param picker            The NPicker.
     * @param numberPickerCount The total NumberPicker count.
     * @param numberPickerIndex The index of the given NumberPicker.
     */
    private void setImeOptions(NumberPicker picker, int numberPickerCount, int numberPickerIndex) {
        final int imeOptions;
        if (numberPickerIndex < numberPickerCount - 1) {
            imeOptions = EditorInfo.IME_ACTION_NEXT;
        } else {
            imeOptions = EditorInfo.IME_ACTION_DONE;
        }
        picker.setImeOptions(imeOptions);
    }

    private void updateInputState() {
        // Make sure that if the user changes the value and the IME is active
        // for one of the inputs if this widget, the IME is closed. If the user
        // changed the value via the IME and there is a next input the IME will
        // be shown, otherwise the user chose another means of changing the
        // value and having the IME up makes no sense.
        // InputMethodManager inputMethodManager =
        // InputMethodManager.peekInstance();
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            if (inputMethodManager.isActive(mYearNPicker)) {
                mYearNPicker.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mMonthNPicker)) {
                mMonthNPicker.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mDayNPicker)) {
                mDayNPicker.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    public void setOnChangedListener(OnChangedListener listener) {
        mOnChangedListener = listener;
    }

    /**
     * The callback used to indicate the user changes\d the date.
     */
    public interface OnChangedListener {

        /**
         * Called upon a date change.
         *
         * @param picker      The view associated with this listener.
         * @param year        The year that was set.
         * @param monthOfYear The month that was set (0-11) for compatibility with
         *                    {@link Calendar}.
         * @param dayOfMonth  The day of the month that was set.
         */
        void onChanged(com.example.datepicker.DatePicker picker, int year, int monthOfYear, int dayOfMonth);
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        @SuppressWarnings("all")
        // suppress unused and hiding
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public com.example.datepicker.DatePicker.SavedState createFromParcel(Parcel in) {
                return new com.example.datepicker.DatePicker.SavedState(in);
            }

            public com.example.datepicker.DatePicker.SavedState[] newArray(int size) {
                return new com.example.datepicker.DatePicker.SavedState[size];
            }
        };
        private final int mYear;
        private final int mMonth;
        private final int mDay;

        /**
         * Constructor called from {@link com.example.datepicker.DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int year, int month, int day) {
            super(superState);
            mYear = year;
            mMonth = month;
            mDay = day;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mYear = in.readInt();
            mMonth = in.readInt();
            mDay = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mYear);
            dest.writeInt(mMonth);
            dest.writeInt(mDay);
        }
    }
    public void setSelectedTextColor(@ColorInt int color) {
        super.setSelectedTextColor(color, mYearNPicker, mMonthNPicker, mDayNPicker);
    }

    public void setDividerColorResource(@ColorRes int colorId) {
        super.setDividerColor(ContextCompat.getColor(getContext(), colorId), mYearNPicker, mMonthNPicker, mDayNPicker);
    }


    public void setDividerThickness(int thickness) {
        super.setDividerThickness(thickness, mYearNPicker, mMonthNPicker, mDayNPicker);
    }

    public void setFormatter(String yearFormatter, String monthFormatter, String dayFormatter) {
        mYearNPicker.setFormatter(yearFormatter);
        mMonthNPicker.setFormatter(monthFormatter);
        mDayNPicker.setFormatter(dayFormatter);
    }
}
