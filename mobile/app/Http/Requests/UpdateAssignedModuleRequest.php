<?php

namespace App\Http\Requests;
use Illuminate\Validation\Rule;
use App\Enums\AssignedModule;
use Illuminate\Foundation\Http\FormRequest;

class UpdateAssignedModuleRequest extends FormRequest
{
    /**
     * Determine if the user is authorized to make this request.
     */
    public function authorize(): bool
    {
        return true;
    }

    /**
     * Get the validation rules that apply to the request.
     *
     * @return array<string, ValidationRule|array<mixed>|string>
     */
    public function rules(): array
    {
        return [
            //
            'assigned_module'=>[
                'required',
            Rule::enum(AssignedModule::class),
        ],];
    }
}
