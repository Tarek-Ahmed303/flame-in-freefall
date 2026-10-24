<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Reset Your Password - PyroNova</title>
</head>

<body style="margin:0; padding:0; background-color:#04060D; font-family:Arial, Helvetica, sans-serif; color:#EEF0F4;">

    {{-- Preheader --}}
    <div style="display:none; max-height:0; overflow:hidden; opacity:0;">
        Reset your PyroNova account password securely.
    </div>

    <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
           style="background-color:#04060D; padding:35px 12px;">
        <tr>
            <td align="center">

                {{-- Main Card --}}
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
                       style="max-width:600px; background-color:#080D1A; border:1px solid #3B3020; border-radius:16px; overflow:hidden;">

                    {{-- Header --}}
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

                    {{-- Title --}}
                    <tr>
                        <td align="center" style="padding:42px 28px 0;">

                            <p style="margin:0; color:#F5A623; font-size:11px; font-weight:bold; letter-spacing:4px; text-transform:uppercase;">
                                Flame in Free Fall
                            </p>

                            <div style="width:42px; height:1px; margin:20px auto; background-color:#7A5A2A;"></div>

                            <h1 style="margin:0; font-family:Georgia, 'Times New Roman', serif; font-size:38px; font-weight:normal; line-height:1.2; color:#EEF0F4;">
                                Reset Your Password
                            </h1>

                            <p style="max-width:420px; margin:20px auto 0; color:#B4BBC9; font-size:14px; line-height:1.8;">
                                Forgot your password? No worries.
                                Use the button below to create a new password
                                and get back to exploring with PyroNova.
                            </p>
                        </td>
                    </tr>

                    {{-- Reset Button --}}
                    <tr>
                        <td align="center" style="padding:36px 28px 0;">

                            <table role="presentation" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td align="center" bgcolor="#E8C27A"
                                        style="border-radius:8px;">
                                        <a href="{{ $url }}"
                                           style="display:inline-block; padding:16px 36px; color:#080D1A; font-size:14px; font-weight:bold; text-decoration:none; letter-spacing:1px;">
                                            RESET PASSWORD
                                        </a>
                                    </td>
                                </tr>
                            </table>

                            <p style="margin:22px 0 0; color:#7D8597; font-size:12px; line-height:1.8;">
                                This password reset link expires in
                                {{ config('auth.passwords.'.config('auth.defaults.passwords').'.expire') }}
                                minutes.
                            </p>

                        </td>
                    </tr>

                  
                    {{-- Security Note --}}
                    <tr>
                        <td align="center" style="padding:30px 28px 38px;">
                            <p style="margin:0; color:#7D8597; font-size:13px; line-height:1.8;">
                                If you didn't request a password reset, you can safely ignore this email.
                                Your password will remain unchanged.
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
