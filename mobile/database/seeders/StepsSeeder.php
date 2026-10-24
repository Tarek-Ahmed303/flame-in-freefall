<?php

namespace Database\Seeders;
use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;
class StepsSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        //
        DB::table('steps')->insert(
            [
            'step_order'=>1,
             'title'=>'Protect Yourself',
             'body'=>'Protect yourself from smoke and hazardous combustion products using approved protective equipment and following emergency procedures.',
       'created_at' => now(),
            'updated_at' => now(),
      
             ],
        [
            'step_order'=>2,
            'title'=>'Report the Fire to the Crew',
            'body'=>'Immediately alert the crew and follow the spacecraft’s fire-reporting and warning procedures',
        ],
           [
            'step_order'=>3,
            'title'=>'Isolate Ignition Sources',
            'body'=>'Isolate affected equipment from its power source only when directed by the spacecraft’s approved emergency procedures',
     'created_at' => now(),
            'updated_at' => now(),
               ],
           [
            'step_order'=>4,
            'title'=>'Suppress the Fire',
            'body'=>'Use the approved fire-suppression equipment according to crew training and spacecraft-specific procedures.',
       'created_at' => now(),
            'updated_at' => now(),
             ],
           [
            'step_order'=>5,
            'title'=>'Check the Atmosphere',
            'body'=>'Check for smoke and harmful combustion products before considering the surrounding atmosphere safe.',
        'created_at' => now(),
            'updated_at' => now(),
            ],
           [
            'step_order'=>6,
            'title'=>'Restore Safe Conditions',
            'body'=>'Resume normal operations only after the atmosphere and affected equipment have been assessed and declared safe under approved procedures.',
       'created_at' => now(),
            'updated_at' => now(),
             ]
        );
    }
}
