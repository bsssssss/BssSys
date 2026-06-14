BssLog : Log {
    set {
        | str, inLevel = \info |
        var logLevel, logItem;
        logLevel = levels[inLevel] ? 0;
        if (logLevel >= levelNum) {
            logItem = (
                \string: str,
                \level: inLevel,
                \time: Date.getDate()
            );
            logItem[\formatted] = this.format(logItem);
            
            this.addEntry(logItem);
            
            if (shouldPost) {
                logItem[\formatted].postln;
            };
            
            actions.do({
                | action |
                action.value(logItem, this);
            });
        }
    }

    warn {
        | str ...items |
        this.set(str.asString.format(*items), \warning)
    }

}
