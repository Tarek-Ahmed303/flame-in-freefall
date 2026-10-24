<?php

use Illuminate\Support\Facades\Route;
use App\Models\Steps;
use App\Http\Controllers\UserProfileController;
use Illuminate\Support\Facades\DB;
use App\Http\Controllers\ChatbotController;
use App\Http\Controllers\ChatController;
use App\Http\Controllers\SimulateFireController;
use App\Http\Controllers\StepsController;
use App\Http\Controllers\UpdateFcmTokenController;
use App\Http\Controllers\UpdateAssignedModuleController;
Route::get('/guide',[StepsController::class,'index']);
Route::put('/reset-module',[UpdateAssignedModuleController::class,'update'])->middleware('auth:sanctum');
Route::put('/fcm-token',[UpdateFcmTokenController::class,'update'])->middleware('auth:sanctum');
Route::post('/simulate',[SimulateFireController::class,'sem'])->middleware('auth:sanctum');
Route::get('/me',[UserProfileController::class,'show'])->middleware('auth:sanctum');
Route::post('/ask',[ChatbotController::class,'sendmessages'])->middleware('auth:sanctum');
Route::get('/chats',[ChatController::class,'index'])->middleware('auth:sanctum');
Route::post('/chats',[ChatController::class,'store'])->middleware('auth:sanctum');
Route::get('/chats/{id}',[ChatController::class,'show'])->middleware('auth:sanctum');