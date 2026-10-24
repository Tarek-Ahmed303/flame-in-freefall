<?php

namespace App\Http\Controllers;

use App\Enums\AssignedModule;
use App\Services\PushNotificationService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class SimulateFireController extends Controller
{
    public function sem(
        Request $request,
        PushNotificationService $pushNotificationService
    ): JsonResponse {
        $user = $request->user();

        if (!$user->assigned_module) {
            return response()->json([
                'message' => 'No module assigned to this user.',
            ], 422);
        }

        $module = $user->assigned_module;

        $pushNotificationService->sendToModule(
            $module,
            'Fire detected-'.$module->value,
            'A fire simulation has been triggered in your module.'
        );

        return response()->json([
            'message' => 'Fire simulation triggered successfully.',
            'module' => $module->value,
        ]);
    }
}
