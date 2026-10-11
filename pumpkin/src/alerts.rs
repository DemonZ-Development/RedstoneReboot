pub struct AlertFormatter;

impl AlertFormatter {
    pub fn format_duration(seconds: u64) -> String {
        if seconds < 60 {
            format!("{}s", seconds)
        } else if seconds < 3600 {
            let minutes = seconds / 60;
            let rem_secs = seconds % 60;
            if rem_secs == 0 {
                format!("{}m", minutes)
            } else {
                format!("{}m {}s", minutes, rem_secs)
            }
        } else {
            let hours = seconds / 3600;
            let rem_minutes = (seconds % 3600) / 60;
            if rem_minutes == 0 {
                format!("{}h", hours)
            } else {
                format!("{}h {}m", hours, rem_minutes)
            }
        }
    }

    pub fn format_template(template: &str, time_str: &str, reason: &str, prefix: &str) -> String {
        let mut result = template.replace("{time}", time_str);
        result = result.replace("{reason}", reason);
        result = result.replace("{prefix}", prefix);
        Self::translate_color_codes(&result)
    }

    pub fn translate_color_codes(text: &str) -> String {
        let chars: Vec<char> = text.chars().collect();
        let mut out = String::with_capacity(chars.len());
        let mut i = 0;

        while i < chars.len() {
            if chars[i] == '&' && i + 1 < chars.len() {
                let next = chars[i + 1].to_ascii_lowercase();
                if next.is_ascii_hexdigit()
                    || next == 'k'
                    || next == 'l'
                    || next == 'm'
                    || next == 'n'
                    || next == 'o'
                    || next == 'r'
                {
                    out.push('§');
                    out.push(next);
                    i += 2;
                    continue;
                }
            }
            out.push(chars[i]);
            i += 1;
        }

        out
    }

    pub fn strip_color_codes(text: &str) -> String {
        let chars: Vec<char> = text.chars().collect();
        let mut out = String::with_capacity(chars.len());
        let mut i = 0;

        while i < chars.len() {
            if (chars[i] == '§' || chars[i] == '&') && i + 1 < chars.len() {
                let next = chars[i + 1].to_ascii_lowercase();
                if next.is_ascii_hexdigit()
                    || next == 'k'
                    || next == 'l'
                    || next == 'm'
                    || next == 'n'
                    || next == 'o'
                    || next == 'r'
                {
                    i += 2;
                    continue;
                }
            }
            out.push(chars[i]);
            i += 1;
        }

        out
    }
}
