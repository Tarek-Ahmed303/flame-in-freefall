{{-- resources/views/emails/registration-otp.blade.php --}}
@php
    $otpDigits = str_split((string) $otp);
@endphp

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Verify Your Identity - PyroNova</title>
</head>

<body style="margin:0; padding:0; background-color:#04060D; font-family:Arial, Helvetica, sans-serif; color:#EEF0F4;">

    {{-- Preheader --}}
    <div style="display:none; max-height:0; overflow:hidden; opacity:0;">
        Your PyroNova verification code expires in 3 minutes.
    </div>

    <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
           style="background-color:#04060D; padding:35px 12px;">
        <tr>
            <td align="center">

                {{-- Main Card --}}
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="max-width:600px; background-color:#080D1A; border:1px solid #3B3020; border-radius:16px; overflow:hidden;">

                    {{-- Header: Flame + PyroNova --}}
                    <tr>
                        <td style="padding:26px 30px; border-bottom:1px solid #292516;">
                            <table role="presentation" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td style="padding-right:10px; font-size:28px; vertical-align:middle;">
                                        🔥
                                    </td>
                                    <td style="font-family:Georgia, 'Times New Roman', serif; font-size:27px; font-weight:bold; color:#EEF0F4; vertical-align:middle;">
                                        Pyro<span style="color:#E8C27A; font-style:italic;">Nova</span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>

                    {{-- Title: Starts immediately after the header --}}
                    <tr>
                        <td align="center" style="padding:42px 28px 0;">

                            <p style="margin:0; color:#F5A623; font-size:11px; font-weight:bold; letter-spacing:4px; text-transform:uppercase;">
                                Flame in Free Fall
                            </p>

                            <div style="width:42px; height:1px; margin:20px auto; background-color:#7A5A2A;"></div>

                            <h1 style="margin:0; font-family:Georgia, 'Times New Roman', serif; font-size:38px; font-weight:normal; line-height:1.2; color:#EEF0F4;">
                                Verify Your Identity
                            </h1>

                            <p style="max-width:420px; margin:20px auto 0; color:#B4BBC9; font-size:14px; line-height:1.8;">
                                You're one step away from joining PyroNova.
                                Enter the verification code below to complete your registration.
                            </p>
                        </td>
                    </tr>

                    {{-- OTP --}}
                    <tr>
                        <td style="padding:32px 24px 0;">

                            <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                                   style="background-color:#0B1222; border:1px solid #39301F; border-radius:14px;">

                                <tr>
                                    <td align="center" style="padding:26px 8px;">

                                        <p style="margin:0 0 22px; color:#7D8597; font-size:10px; letter-spacing:3px; text-transform:uppercase;">
                                            Verification Code
                                        </p>

                                        <table role="presentation" align="center" cellpadding="0" cellspacing="0">
                                            <tr>
                                                @foreach ($otpDigits as $digit)
                                                    <td style="padding:0 4px;">
                                                        <table role="presentation" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                <td align="center" valign="middle"
                                                                    style="width:48px; height:62px; background-color:#0E1526; border:1px solid #705526; border-radius:9px; color:#FFD9A0; font-family:'Courier New', monospace; font-size:29px; font-weight:bold;">
                                                                    {{ $digit }}
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                @endforeach
                                            </tr>
                                        </table>

                                        <p style="margin:20px 0 0; color:#7D8597; font-size:12px;">
                                            Or copy:
                                            <span style="color:#E8C27A; font-family:'Courier New', monospace; font-size:15px; letter-spacing:3px;">
                                                {{ $otp }}
                                            </span>
                                        </p>

                                    </td>
                                </tr>

                                {{-- Expiration --}}
                                <tr>
                                    <td align="center"
                                        style="padding:15px 10px; background-color:#17150F; border-top:1px solid #39301F;">

                                        <span style="color:#F5A623;">&#9679;</span>

                                        <span style="color:#F7C46C; font-size:13px; font-weight:bold;">
                                            This code expires in 3 minutes.
                                        </span>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>

                    {{-- Security Note --}}
                    <tr>
                        <td align="center" style="padding:30px 28px 38px;">
                            <p style="margin:0; color:#7D8597; font-size:13px; line-height:1.8;">
                                If you didn't request this code, you can safely ignore this email.
                            </p>
                        </td>
                    </tr>

                    {{-- Footer --}}
                    <tr>
                        <td align="center"
                            style="padding:28px 20px; background-color:#060A14; border-top:1px solid #292516;">

                            <p style="margin:0; font-family:Georgia, 'Times New Roman', serif; font-size:18px;">
                                <span style="color:#EEF0F4; font-weight:bold;">PyroNova</span>
                                <span style="color:#5A4A33;"> | </span>
                                <span style="color:#E8C27A; font-style:italic;">
                                    Exploring combustion beyond Earth
                                </span>
                            </p>

                            <p style="margin:14px 0 0; color:#7D8597; font-size:10px; line-height:1.8; letter-spacing:1px;">
                                COMBUSTION RESEARCH &bull; MICROGRAVITY &bull; SPACE EXPLORATION
                            </p>

                            <p style="margin:18px 0 0; color:#5C6375; font-size:11px;">
                                This is an automated message. Please do not reply.
                            </p>

                        </td>
                    </tr>

                </table>
            </td>
        </tr>
    </table>

</body>
</html>