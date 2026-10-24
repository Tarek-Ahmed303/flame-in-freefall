<?php

use Illuminate\Foundation\Application;
use Illuminate\Foundation\Configuration\Exceptions;
use Illuminate\Foundation\Configuration\Middleware;
use Illuminate\Http\Request;

return Application::configure(basePath: dirname(__DIR__))
   ->withRouting(
    web: __DIR__.'/../routes/web.php',
    api: __DIR__.'/../routes/api.php',
    apiPrefix: '',
    commands: __DIR__.'/../routes/console.php',
    health: '/up',
)
    ->withMiddleware(function (Middleware $middleware): void {
        //
        $middleware->validateCsrfTokens(except:[
            'products','students','books' ,
            'books/*','doctors','doctors/*',
            'appointments','appointments/*',
            'register/*','register','/login',
            'me','/me','me/*','/logout','logout',
            'generate-otp/*','/chats',
            'verify-email','verify-email/*'
        ,'fcm-token','/reset-module','simulate','/me','/ask'
            ]);
        
    })
    ->withExceptions(function (Exceptions $exceptions): void {
        $exceptions->shouldRenderJsonWhen(
            fn (Request $request) => $request->is('api/*') || $request->expectsJson(),
        );
    })->create();
