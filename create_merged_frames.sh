../../convert_sim_frames.sh $1
../../convert_net_frames.sh $1
../../../saliency/bin/net-sim-mergeFrame $1 main_recurrentInput main_input main_hebbian main_hidden main_output main_recurrentOutput
rm $1-*.ppm
rm $1_main_*.ppm
rm $1_audio_*.ppm
