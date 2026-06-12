export type BookingErrorKind = 'selfOverlap' | 'alreadyBooked' | 'generic';

export interface ClassifiedBookingError {
    kind: BookingErrorKind;
    userMessage: string;
}

export function classifyBookingError(rawMessage: string): ClassifiedBookingError {
    const m = (rawMessage ?? '').trim();

    if (/already have a booking that overlaps this time slot/i.test(m) ||
        /Hai già una prenotazione che si sovrappone/i.test(m)) {
        return {
            kind: 'selfOverlap',
            userMessage:
                'Hai già una prenotazione che si sovrappone a questa fascia. Controlla «Le mie prenotazioni» per modificarla o cancellarla, oppure scegli un altro orario.'
        };
    }

    if (/Desk is already booked for the selected time range/i.test(m) ||
        /La postazione risulta già occupata/i.test(m)) {
        return {
            kind: 'alreadyBooked',
            userMessage: 'La postazione risulta occupata in questa fascia. Vuoi essere avvisato se si libera?'
        };
    }

    const authLike = /invalid token/i.test(m) ||
        /jwt expired/i.test(m) ||
        /token expired/i.test(m) ||
        /full authentication is required/i.test(m);

    const userMessage =
        m === 'startTime must be in the future'
            ? 'La fascia di inizio non è più prenotabile rispetto all’ora corrente. Scegli un orario successivo.'
            : m === 'endTime must be in the future'
                ? 'La fascia di fine non è più valida rispetto all’ora corrente.'
                : /Could not complete booking due to a concurrent conflict/i.test(m)
                    ? 'Impossibile completare la prenotazione: conflitto concorrente. Riprova tra pochi secondi.'
                    : authLike
                        ? 'Sessione scaduta o non valida. Effettua di nuovo l’accesso e riprova.'
                        : m || 'Prenotazione non riuscita.';

    return { kind: 'generic', userMessage };
}
