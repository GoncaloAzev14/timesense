export function countBusinessDays(startDate: Date, endDate: Date, holidays: Date[], vacationType?: string): number {
    let count = 0;
    let current = new Date(startDate);

    // Set holidays array to YYYY-MM-DD
    const holidaySet = new Set(holidays.map(date => date.toDateString()));

    while (current <= endDate) {
        const day = current.getDay(); // 0 = Sunday, 6 = Saturday
        const isWeekend = day === 0 || day === 6;
        const isHoliday = holidaySet.has(current.toDateString());

        if (!isWeekend && !isHoliday) {
            count++;
        }

        // Advance 1 day
        current.setDate(current.getDate() + 1);
    }

    if (vacationType === 'Morning' || vacationType === 'Afternoon') {
        count = count / 2;
    }

    return count;
}