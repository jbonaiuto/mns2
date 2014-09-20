../../convert_sim_frames.sh $1
../../convert_net_frames.sh $2
../../../saliency/bin/net-sim-mergeFrame $1 $2 main_recurrentInput main_input main_hidden main_output main_recurrentOutput
../../create_movie.sh ../../../saliency merged-$2
mv ./merged-$2.mpg ./$2.mpg
rm merged-$2*.ppm
rm $1-*.ppm
rm $2_main_*.ppm
rm $2_audio_*.ppm
