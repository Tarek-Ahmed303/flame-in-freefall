<?php

namespace App\Http\Controllers;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use App\Http\Requests\UpdateAssignedModuleRequest;
class UpdateAssignedModuleController extends Controller
{
    //
    public function update(UpdateAssignedModuleRequest $request):JsonResponse{
     $user = $request->user();
     $assigned_module=$request->validated('assigned_module');
        $user->update([
            'assigned_module'=>$assigned_module
        ]);

        return response()->json([
            'message'=>"Assigned Module Updated Successfully"
        ]);
    }
}
