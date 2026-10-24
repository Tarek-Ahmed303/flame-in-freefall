<?php

namespace App\Http\Controllers;

use App\Models\Steps;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
class StepsController extends Controller
{
    //
    public function index(Request $request){
return DB::table('steps')->orderBy('step_order')->get();
    }
}
